/**
 * Centralized API service for all authenticated requests
 * Handles token management, authorization headers, and error handling
 */

// Constants
export const TOKEN_KEY = 'chicken_admin_token';
const API_BASE = '/api/admin';

// Types
export interface ApiResponse<T = any> {
  ok: boolean;
  data?: T;
  error?: string;
  status?: number;
}

export interface LocationRequest {
  name: string;
  address?: string;
  managerName: string;
  managerEmail: string;
}

export interface LocationResponse {
  id: number;
  message: string;
  status: string;
  slug: string;
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

class ApiService {
  private csrfToken: string | null = null;

  /**
   * Get the authentication token from sessionStorage
   */
  private getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  /**
   * Get CSRF token from cookie
   */
  private getCsrfTokenFromCookie(): string | null {
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookieArray = decodedCookie.split(';');
    
    for (let cookie of cookieArray) {
      cookie = cookie.trim();
      if (cookie.indexOf(name) === 0) {
        return cookie.substring(name.length, cookie.length);
      }
    }
    return null;
  }

  /**
   * Fetch CSRF token from server
   */
  private async fetchCsrfToken(): Promise<string | null> {
    try {
      const response = await fetch(`${API_BASE}/auth/csrf-token`, {
        method: 'GET',
        credentials: 'include', // Important for CSRF cookies
      });
      
      if (response.ok) {
        const data: CsrfTokenResponse = await response.json();
        this.csrfToken = data.token;
        return data.token;
      }
    } catch (error) {
      console.error('Failed to fetch CSRF token:', error);
    }
    return null;
  }

  /**
   * Get CSRF token (from cookie or server)
   */
  private async getCsrfToken(): Promise<string | null> {
    // First try to get from cookie
    let token = this.getCsrfTokenFromCookie();
    
    // If not in cookie, try cached token
    if (!token && this.csrfToken) {
      token = this.csrfToken;
    }
    
    // If still no token, fetch from server
    if (!token) {
      token = await this.fetchCsrfToken();
    }
    
    return token;
  }

  /**
   * Create headers with authentication token and CSRF token
   */
  private async getHeaders(includeAuth: boolean = true, includeCsrf: boolean = false): Promise<HeadersInit> {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (includeAuth) {
      const token = this.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    if (includeCsrf) {
      const csrfToken = await this.getCsrfToken();
      if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
      }
    }

    return headers;
  }

  /**
   * Handle API response and errors
   */
  private async handleResponse<T>(response: Response): Promise<ApiResponse<T>> {
    if (response.ok) {
      try {
        const data = await response.json();
        return { ok: true, data };
      } catch {
        // Response might be empty
        return { ok: true };
      }
    }

    // Handle error responses
    const status = response.status;
    let error = `Request failed with status ${status}`;

    // Handle authentication errors
    if (status === 401) {
      // Token expired or invalid - remove it
      sessionStorage.removeItem(TOKEN_KEY);
      error = 'Authentication expired. Please login again.';
      // Redirect to login
      window.location.href = '/admin/login';
    } else if (status === 403) {
      error = 'Access denied. You do not have permission to perform this action.';
    }

    // Try to get error message from response
    try {
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const errorData = await response.json();
        error = errorData.message || errorData.error || error;
      } else {
        const text = await response.text();
        if (!text.includes('<!DOCTYPE') && !text.includes('<html')) {
          error = text || error;
        }
      }
    } catch {
      // Ignore parsing errors
    }

    return { ok: false, error, status };
  }

  /**
   * Make a GET request
   */
  async get<T>(endpoint: string, includeAuth: boolean = true): Promise<ApiResponse<T>> {
    try {
      const headers = await this.getHeaders(includeAuth, false);
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'GET',
        headers,
        credentials: 'include', // Include cookies for CSRF
      });
      return this.handleResponse<T>(response);
    } catch (error) {
      return {
        ok: false,
        error: error instanceof Error ? error.message : 'Network error',
      };
    }
  }

  /**
   * Make a POST request
   */
  async post<T>(endpoint: string, data?: any, includeAuth: boolean = true): Promise<ApiResponse<T>> {
    try {
      // Include CSRF for all POST requests except login and CSRF token retrieval
      const needsCsrf = !endpoint.includes('/auth/login') && !endpoint.includes('/auth/csrf-token');
      const headers = await this.getHeaders(includeAuth, needsCsrf);
      
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers,
        credentials: 'include', // Include cookies for CSRF
        body: data ? JSON.stringify(data) : undefined,
      });
      return this.handleResponse<T>(response);
    } catch (error) {
      return {
        ok: false,
        error: error instanceof Error ? error.message : 'Network error',
      };
    }
  }

  /**
   * Make a DELETE request
   */
  async delete<T>(endpoint: string, includeAuth: boolean = true): Promise<ApiResponse<T>> {
    try {
      const headers = await this.getHeaders(includeAuth, true); // DELETE requests need CSRF
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'DELETE',
        headers,
        credentials: 'include', // Include cookies for CSRF
      });
      return this.handleResponse<T>(response);
    } catch (error) {
      return {
        ok: false,
        error: error instanceof Error ? error.message : 'Network error',
      };
    }
  }

  // Specific API methods

  /**
   * Initialize CSRF token (call after successful login)
   */
  async initializeCsrfToken(): Promise<void> {
    await this.getCsrfToken();
  }
  
  /**
   * Login (no auth required)
   */
  async login(email: string, password: string) {
    const result = this.post('/auth/login', { email, password }, false);
    // After successful login, initialize CSRF token for subsequent requests
    if ((await result).ok) {
      await this.initializeCsrfToken();
    }
    return result;
  }

  /**
   * Validate token
   */
  async validateToken() {
    return this.post('/auth/validate');
  }

  /**
   * Logout
   */
  async logout() {
    return this.post('/auth/logout');
  }

  /**
   * Get dashboard stats
   */
  async getDashboardStats() {
    return this.get('/stats');
  }

  /**
   * Get all locations
   */
  async getLocations() {
    return this.get('/locations');
  }

  /**
   * Create a new location
   */
  async createLocation(data: LocationRequest): Promise<ApiResponse<LocationResponse>> {
    return this.post<LocationResponse>('/locations', data);
  }

  /**
   * Delete a location
   */
  async deleteLocation(id: string) {
    return this.delete(`/locations/${id}`);
  }

  /**
   * Change password
   */
  async changePassword(data: PasswordChangeRequest) {
    return this.post('/auth/change-password', data);
  }
}

// Export singleton instance
const apiService = new ApiService();
export default apiService;