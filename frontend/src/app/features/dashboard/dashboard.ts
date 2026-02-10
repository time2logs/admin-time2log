import {Component, inject, OnInit} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './dashboard.html',
})
export class DashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  profile$ = this.authService.currentProfile$;

  ngOnInit() {

  }
}
