import { Component } from '@angular/core';
import { LayoutComponent } from './shared/layout/layout';
import { ToastComponent } from './shared/toast/toast';

@Component({
  selector: 'app-root',
  imports: [LayoutComponent, ToastComponent],
  templateUrl: './app.html',
})
export class App {}
