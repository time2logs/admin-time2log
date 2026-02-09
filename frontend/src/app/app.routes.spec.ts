import { routes } from './app.routes';

describe('Routes', () => {
  it('should be defined as an array', () => {
    expect(routes).toBeDefined();
    expect(Array.isArray(routes)).toBeTrue();
  });
});
