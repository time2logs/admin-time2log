import {inject, Injectable, OnDestroy} from '@angular/core';
import {
  AuthChangeEvent,
  AuthResponse,
  createClient,
  Session,
  SupabaseClient,
  User
} from '@supabase/supabase-js';
import { navigatorLock } from '@supabase/auth-js/dist/module/lib/locks';
import { environment } from '@env/environment';
import { BehaviorSubject, from, Observable } from 'rxjs';
import {Profile} from '@app/core/models/profile.models';
import {HttpClient} from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private supabase: SupabaseClient;
  private readonly http = inject(HttpClient);

  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  private readonly initializedSubject = new BehaviorSubject<boolean>(false);
  readonly isInitialized$ = this.initializedSubject.asObservable();

  private currentProfileSubject = new BehaviorSubject<Profile | null>(null);
  readonly currentProfile$ = this.currentProfileSubject.asObservable();

  private authSubscription?: { unsubscribe: () => void };

  constructor() {
    this.supabase = createClient(environment.supabaseUrl, environment.supabaseKey, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
        lock: (name, _acquireTimeout, fn) => navigatorLock(name, 5000, fn),
      },
    });
    this.currentProfileSubject = new BehaviorSubject<Profile | null>(null);
    this.currentProfile$ = this.currentProfileSubject.asObservable();
    this.initializeAuth();
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  private async initializeAuth() {
    const { data, error } = await this.supabase.auth.getUser();
    if (!error && data.user) {
      this.currentUserSubject.next(data.user);
      this.loadProfile();
    } else {
      this.currentUserSubject.next(null);
      await this.supabase.auth.signOut();
    }

    this.authSubscription = this.supabase.auth.onAuthStateChange(
      (event: AuthChangeEvent, session: Session | null) => {
        this.currentUserSubject.next(session?.user ?? null);
        if (this.currentUserSubject.value !== null) {
          this.loadProfile();
        }
      }
    ).data.subscription;

    this.initializedSubject.next(true);
  }

  isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  async getAccessToken(): Promise<string | null> {
    const { data: { session } } = await this.supabase.auth.getSession();
    if (!session) return null;

    const expiresAt = session.expires_at ?? 0;
    const now = Math.floor(Date.now() / 1000);
    const bufferSeconds = 30;

    if (expiresAt - now > bufferSeconds) {
      return session.access_token;
    }

    const { data, error } = await this.supabase.auth.refreshSession();
    if (error || !data.session) {
      this.currentUserSubject.next(null);
      await this.supabase.auth.signOut();
      return null;
    }

    return data.session.access_token;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return from(this.supabase.auth.signInWithPassword({ email, password }));
  }

  register(email: string, password: string, firstName: string, lastName: string): Observable<AuthResponse> {
    return from(
      this.supabase.auth.signUp({
        email,
        password,
        options: { data: { first_name: firstName, last_name: lastName } },
      })
    );
  }

  logout(): Observable<void> {
    this.currentUserSubject.next(null);
    return from(this.supabase.auth.signOut().then(() => undefined));
  }

  deleteProfile(): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/profile`);
  }

  loadProfile() {
    this.http.get<Profile>(`${environment.apiBaseUrl}/profile`).subscribe({
      next: (profile) => {
        this.currentProfileSubject.next(profile);
      },
      error: (err) => console.error('Failed to load profile:', err)
    });
  }
}
