declare const ENV_API_BASE_URL: string;
declare const ENV_SUPABASE_URL: string;
declare const ENV_SUPABASE_PUBLISHABLE_KEY: string;

export const environment = {
  production: true,
  apiBaseUrl: ENV_API_BASE_URL + '/api',
  supabaseUrl: ENV_SUPABASE_URL || 'https://rthgesnneqwvzctganyz.supabase.co',
  supabaseKey: ENV_SUPABASE_PUBLISHABLE_KEY || 'sb_publishable_DsFsBT9lbEOWaevZL3EkJw_48dtJ2Ur',
};
