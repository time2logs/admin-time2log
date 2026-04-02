import { Component, inject } from '@angular/core';
import { LayoutComponent } from './shared/layout/layout';
import { ToastComponent } from './shared/toast/toast';
import { ThemeService } from '@services/theme.service';

@Component({
  selector: 'app-root',
  imports: [LayoutComponent, ToastComponent],
  templateUrl: './app.html',
})
export class App {
  private readonly theme = inject(ThemeService);
}
