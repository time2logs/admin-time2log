import { Injectable, OnDestroy } from '@angular/core';
import {
  AuthChangeEvent,
  AuthResponse,
  createClient,
  Session,
  SupabaseClient,
  User
} from '@supabase/supabase-js';
import { environment } from '@env/environment';
import { BehaviorSubject, from, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private supabase: SupabaseClient;

  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  private readonly initializedSubject = new BehaviorSubject<boolean>(false);
  readonly isInitialized$ = this.initializedSubject.asObservable();

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
  }

  private async initializeAuth() {
    const { data, error } = await this.supabase.auth.getSession();
    if (!error) {
      this.currentUserSubject.next(data.session?.user ?? null);
    } else {
      this.currentUserSubject.next(null);
    }

    this.authSubscription = this.supabase.auth.onAuthStateChange(
      (event: AuthChangeEvent, session: Session | null) => {
        this.currentUserSubject.next(session?.user ?? null);
      }
    ).data.subscription;

    this.initializedSubject.next(true);
  }

  isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  async getAccessToken(): Promise<string | null> {
    const { data } = await this.supabase.auth.getSession();
    return data.session?.access_token ?? null;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return from(this.supabase.auth.signInWithPassword({ email, password }));
  }

  register(email: string, password: string, name: string): Observable<AuthResponse> {
    return from(
      this.supabase.auth.signUp({
        email,
        password,
        options: { data: { name } },
      })
    );
  }

  logout(): Observable<void> {
    this.currentUserSubject.next(null);
    return from(this.supabase.auth.signOut().then(() => undefined));
  }
}
