import {Component, inject, OnInit, signal} from '@angular/core';
import {Profile} from '@app/core/models/profile.models';
import { SystemAdminService } from '@services/system-admin.service';
import {FormsModule} from '@angular/forms';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {Invite} from '@app/core/models/system-admin.models';
import {ToastService} from '@services/toast.service';

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
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  protected readonly admins = signal<Profile[]>([]);
  protected readonly isLoadingAdmins = signal(false);
  protected readonly moderators = signal<Profile[]>([]);
  protected readonly isLoadingModerators = signal(false);
  protected readonly inviteEmail = signal('');
  protected readonly isInviting = signal(false);
  protected readonly inviteError = signal(false);
  protected readonly inviteSuccess = signal(false);
  protected readonly showDeleteConfirm = signal(false);
  protected readonly invites = signal<Invite[]>([]);

  private static readonly EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  ngOnInit(): void {
    this.loadAdmins();
    this.loadModerators();
    this.loadInvites();
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
        this.loadInvites();
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

  private loadModerators(): void {
    this.isLoadingModerators.set(true);
    this.systemAdminService.getModerators().subscribe({
      next: (moderators) => {
        this.moderators.set(moderators);
        this.isLoadingModerators.set(false);
      },
      error: () => this.isLoadingModerators.set(false),
    });
  }

  protected cancelInvite(invite: Invite): void {
    this.systemAdminService.deleteInvite(invite.id).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.inviteCancelled'));
        this.loadInvites();
      },
    });
  }

  private loadInvites(): void {
    this.systemAdminService.listInvites("admin").subscribe({
      next: (invites) => this.invites.set(invites),
    });
  }
}
