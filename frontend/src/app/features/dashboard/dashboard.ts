import {Component, inject, OnInit} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './dashboard.html',
})
export class DashboardComponent {

}
