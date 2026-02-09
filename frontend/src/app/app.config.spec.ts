import { appConfig } from './app.config';

describe('AppConfig', () => {
  it('should have providers defined', () => {
    expect(appConfig.providers).toBeDefined();
    expect(appConfig.providers.length).toBeGreaterThan(0);
  });
});
