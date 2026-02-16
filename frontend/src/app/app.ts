import { LayoutComponent } from './shared/layout/layout';
import {HTTP_INTERCEPTORS} from '@angular/common/http';
import {Component} from '@angular/core';
import {ErrorInterceptor} from '../../interceptors/error.interceptor';


@Component({
  selector: 'app-root',
  imports: [LayoutComponent],
  templateUrl: './app.html',
  providers: [
    {provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true}
  ]
})

export class App {
}
