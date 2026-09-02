import {inject, Injectable, OnDestroy} from '@angular/core';
import {
  AuthChangeEvent,
  AuthResponse,
  createClient,
  Session,
  SupabaseClient,
  User
} from '@supabase/supabase-js';
import { environment } from '@env/environment';
import { BehaviorSubject, from, Observable, Subscription } from 'rxjs';
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
  private profileLoadedForUserId: string | null = null;
  private profileLoadingForUserId: string | null = null;
  private profileSubscription?: Subscription;

  private authSubscription?: { unsubscribe: () => void };

  constructor() {
    this.supabase = createClient(environment.supabaseUrl, environment.supabaseKey, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    });
    this.initializeAuth();
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
    this.profileSubscription?.unsubscribe();
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
        if (session?.user) {
          this.loadProfile(session.user.id);
        } else {
          this.clearProfile();
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

  async signInWithGoogle(redirectPath = '/dashboard'): Promise<void> {
    const safePath = redirectPath.startsWith('/') && !redirectPath.startsWith('//') ? redirectPath : '/dashboard';
    const { error } = await this.supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: window.location.origin + safePath,
      },
    });
    if (error) throw error;
  }

  resetPasswordForEmail(email: string): Promise<{ error: Error | null }> {
    return this.supabase.auth.resetPasswordForEmail(email, {
      redirectTo: window.location.origin + '/auth/reset-password',
    });
  }

  updatePassword(password: string): Promise<{ error: Error | null }> {
    return this.supabase.auth.updateUser({ password }).then(({ error }) => ({ error }));
  }

  updateEmail(email: string): Promise<{ error: Error | null }> {
    return this.supabase.auth.updateUser({ email }).then(({ error }) => ({ error }));
  }

  logout(): Observable<void> {
    this.currentUserSubject.next(null);
    this.clearProfile();
    return from(this.supabase.auth.signOut().then(() => undefined));
  }

  deleteProfile(): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/profile`);
  }

  loadProfile(userId = this.currentUserSubject.value?.id ?? null): void {
    if (!userId || this.profileLoadedForUserId === userId || this.profileLoadingForUserId === userId) {
      return;
    }

    if (this.profileLoadedForUserId && this.profileLoadedForUserId !== userId) {
      this.currentProfileSubject.next(null);
    }
    this.profileSubscription?.unsubscribe();
    this.profileLoadingForUserId = userId;
    this.profileSubscription = this.http.get<Profile>(`${environment.apiBaseUrl}/profile`).subscribe({
      next: (profile) => {
        this.profileLoadedForUserId = userId;
        this.profileLoadingForUserId = null;
        this.currentProfileSubject.next(profile);
      },
      error: (err) => {
        this.profileLoadingForUserId = null;
        console.error('Failed to load profile:', err);
      },
    });
  }

  private clearProfile(): void {
    this.profileSubscription?.unsubscribe();
    this.profileLoadedForUserId = null;
    this.profileLoadingForUserId = null;
    this.currentProfileSubject.next(null);
  }
}
