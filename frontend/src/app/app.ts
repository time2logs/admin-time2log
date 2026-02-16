import { LayoutComponent } from './shared/layout/layout';
import {Component} from '@angular/core';

@Component({
  selector: 'app-root',
  imports: [LayoutComponent],
  templateUrl: './app.html',
  providers: []
})

export class App {
}
