import {Component, inject, OnInit, signal} from '@angular/core';
import {Profile} from '@app/core/models/profile.models';
import { SystemAdminService } from '@services/system-admin.service';
import {FormsModule} from '@angular/forms';
import {TranslatePipe} from '@ngx-translate/core';

@Component({
  selector: 'app-system-admin',
  imports: [
    FormsModule,
    TranslatePipe
  ],
  templateUrl: './system-admin.html',
  standalone: true,
})
export class SystemAdmin implements OnInit {
  private readonly systemAdminService = inject(SystemAdminService);

  protected readonly admins = signal<Profile[]>([]);
  protected readonly isLoadingAdmins = signal(false);
  protected readonly inviteEmail = signal('');
  protected readonly isInviting = signal(false);
  protected readonly inviteError = signal(false);
  protected readonly inviteSuccess = signal(false);

  private static readonly EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  ngOnInit(): void {
    this.loadAdmins();
  }

  protected isEmailValid(): boolean {
    return SystemAdmin.EMAIL_REGEX.test(this.inviteEmail().trim());
  }

  protected invite(): void {
    const email = this.inviteEmail().trim();
    if (!this.isEmailValid()) {
      this.inviteError.set(true);
      return;
    }

    this.isInviting.set(true);
    this.inviteError.set(false);
    this.inviteSuccess.set(false);

    this.systemAdminService.createInvite(email).subscribe({
      next: () => {
        this.inviteEmail.set('');
        this.inviteSuccess.set(true);
        this.isInviting.set(false);
        this.loadAdmins();
      },
      error: () => {
        this.inviteError.set(true);
        this.isInviting.set(false);
      },
    });
  }

  private loadAdmins(): void {
    this.isLoadingAdmins.set(true);
    this.systemAdminService.getAdmins().subscribe({
      next: (admins) => {
        this.admins.set(admins);
        this.isLoadingAdmins.set(false);
      },
      error: () => this.isLoadingAdmins.set(false),
    });
  }
}
