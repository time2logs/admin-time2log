import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import {HeaderComponent} from '../header/header';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, TranslateModule, HeaderComponent],
  templateUrl: './layout.html',
})
export class LayoutComponent {}
