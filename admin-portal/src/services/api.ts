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

class ApiService {
  /**
   * Get the authentication token from sessionStorage
   */
  private getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  /**
   * Create headers with authentication token
   */
  private getHeaders(includeAuth: boolean = true): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (includeAuth) {
      const token = this.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
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
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'GET',
        headers: this.getHeaders(includeAuth),
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
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: this.getHeaders(includeAuth),
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
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'DELETE',
        headers: this.getHeaders(includeAuth),
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
   * Login (no auth required)
   */
  async login(email: string, password: string) {
    return this.post('/auth/login', { email, password }, false);
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
}

// Export singleton instance
const apiService = new ApiService();
export default apiService;