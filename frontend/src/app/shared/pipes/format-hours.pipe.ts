import { Pipe, PipeTransform } from '@angular/core';
import { formatHours } from '@app/shared/utils/format-hours.utils';

@Pipe({
  name: 'formatHours',
  standalone: true,
})
export class FormatHoursPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatHours(value);
  }
}
