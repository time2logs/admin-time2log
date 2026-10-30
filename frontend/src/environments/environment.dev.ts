declare const ENV_API_BASE_URL: string;
declare const ENV_SUPABASE_URL: string;
declare const ENV_SUPABASE_PUBLISHABLE_KEY: string;

export const environment = {
  production: false,
  apiBaseUrl: ENV_API_BASE_URL + '/api',
  supabaseUrl: ENV_SUPABASE_URL || 'https://icakwycwpldldvcyhpqe.supabase.co',
  supabaseKey: ENV_SUPABASE_PUBLISHABLE_KEY || 'sb_publishable_e8-f5Xz1f9qimbSb1kaQ5w_1sAAGvnh',
};
