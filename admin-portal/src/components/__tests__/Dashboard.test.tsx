import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import Dashboard from '../Dashboard';
import apiService from '../../services/api';

// Mock the API service
jest.mock('../../services/api');
const mockApiService = apiService as jest.Mocked<typeof apiService>;

// Mock data
const mockLocations = [
  {
    id: '1',
    name: 'Downtown Restaurant',
    domain: 'downtown.chickencalc.com',
    status: 'active' as const,
    lastSeen: '2024-01-15T10:30:00Z',
    managerEmail: 'manager@downtown.com',
    cloudProvider: 'AWS',
    region: 'us-east-1',
    createdAt: '2024-01-01T00:00:00Z'
  },
  {
    id: '2',
    name: 'Airport Location',
    domain: 'airport.chickencalc.com',
    status: 'deploying' as const,
    lastSeen: '2024-01-15T09:45:00Z',
    managerEmail: 'manager@airport.com',
    cloudProvider: 'GCP',
    region: 'us-central1',
    createdAt: '2024-01-02T00:00:00Z'
  },
  {
    id: '3',
    name: 'Mall Food Court',
    domain: 'mall.chickencalc.com',
    status: 'error' as const,
    lastSeen: '2024-01-14T15:20:00Z',
    managerEmail: 'manager@mall.com',
    cloudProvider: 'Azure',
    region: 'eastus',
    createdAt: '2024-01-03T00:00:00Z'
  }
];

const mockStats = {
  totalLocations: 3,
  activeLocations: 1,
  deployingLocations: 1,
  errorLocations: 1,
  totalTransactions: 1250,
  totalRevenue: 15750
};

describe('Dashboard Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset timers for setInterval
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('Loading State', () => {
    test('shows loading spinner while fetching data', () => {
      // Mock API calls to never resolve
      mockApiService.getLocations.mockReturnValue(new Promise(() => {}));
      mockApiService.getDashboardStats.mockReturnValue(new Promise(() => {}));

      render(<Dashboard />);

      expect(screen.getByRole('progressbar', { hidden: true })).toBeInTheDocument();
    });
  });

  describe('Successful Data Loading', () => {
    beforeEach(() => {
      mockApiService.getLocations.mockResolvedValue({
        ok: true,
        data: mockLocations,
        error: undefined
      });
      mockApiService.getDashboardStats.mockResolvedValue({
        ok: true,
        data: mockStats,
        error: undefined
      });
    });

    test('renders dashboard title and description', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
      });

      expect(screen.getByText(/overview of all chicken calculator locations/i)).toBeInTheDocument();
    });

    test('displays correct statistics cards', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByLabelText(/3 total locations/i)).toBeInTheDocument();
      });

      expect(screen.getByLabelText(/1 active locations/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/1,250 total transactions/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/\$15,750 total revenue/i)).toBeInTheDocument();
    });

    test('shows alert when there are error locations', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      expect(screen.getByText(/1 location\(s\) need attention/i)).toBeInTheDocument();
      expect(screen.getByText(/check the locations list below/i)).toBeInTheDocument();
    });

    test('renders locations table with correct data', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByRole('table')).toBeInTheDocument();
      });

      // Check table headers
      expect(screen.getByRole('columnheader', { name: /location/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /status/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /manager/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /infrastructure/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /last seen/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /actions/i })).toBeInTheDocument();

      // Check location data
      expect(screen.getByText('Downtown Restaurant')).toBeInTheDocument();
      expect(screen.getByText('Airport Location')).toBeInTheDocument();
      expect(screen.getByText('Mall Food Court')).toBeInTheDocument();

      // Check status indicators
      expect(screen.getByText('active')).toBeInTheDocument();
      expect(screen.getByText('deploying')).toBeInTheDocument();
      expect(screen.getByText('error')).toBeInTheDocument();
    });

    test('renders action buttons for each location', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getAllByRole('link', { name: /visit .* location/i })).toHaveLength(3);
      });

      expect(screen.getAllByRole('button', { name: /manage .* location/i })).toHaveLength(3);
      expect(screen.getAllByRole('button', { name: /delete .* location/i })).toHaveLength(3);
    });

    test('external links have correct attributes', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        const visitLinks = screen.getAllByRole('link', { name: /visit .* location/i });
        visitLinks.forEach(link => {
          expect(link).toHaveAttribute('target', '_blank');
          expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        });
      });
    });
  });

  describe('Empty State', () => {
    beforeEach(() => {
      mockApiService.getLocations.mockResolvedValue({
        ok: true,
        data: [],
        error: undefined
      });
      mockApiService.getDashboardStats.mockResolvedValue({
        ok: true,
        data: {
          totalLocations: 0,
          activeLocations: 0,
          deployingLocations: 0,
          errorLocations: 0,
          totalTransactions: 0,
          totalRevenue: 0
        },
        error: undefined
      });
    });

    test('shows empty state when no locations exist', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByText(/no locations yet/i)).toBeInTheDocument();
      });

      expect(screen.getByText(/get started by creating your first location/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /create your first location/i })).toBeInTheDocument();
    });

    test('does not show error alert when no error locations', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByText(/no locations yet/i)).toBeInTheDocument();
      });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('Error Handling', () => {
    test('handles API errors gracefully', async () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      mockApiService.getLocations.mockResolvedValue({
        ok: false,
        data: null,
        error: 'Failed to fetch locations'
      });
      mockApiService.getDashboardStats.mockResolvedValue({
        ok: false,
        data: null,
        error: 'Failed to fetch stats'
      });

      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.queryByRole('progressbar', { hidden: true })).not.toBeInTheDocument();
      });

      expect(consoleErrorSpy).toHaveBeenCalledWith('Error fetching locations:', 'Failed to fetch locations');
      expect(consoleErrorSpy).toHaveBeenCalledWith('Error fetching stats:', 'Failed to fetch stats');

      consoleErrorSpy.mockRestore();
    });

    test('handles network errors gracefully', async () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      mockApiService.getLocations.mockRejectedValue(new Error('Network error'));
      mockApiService.getDashboardStats.mockRejectedValue(new Error('Network error'));

      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.queryByRole('progressbar', { hidden: true })).not.toBeInTheDocument();
      });

      expect(consoleErrorSpy).toHaveBeenCalledWith('Error fetching dashboard data:', expect.any(Error));

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Auto-refresh Functionality', () => {
    beforeEach(() => {
      mockApiService.getLocations.mockResolvedValue({
        ok: true,
        data: mockLocations,
        error: undefined
      });
      mockApiService.getDashboardStats.mockResolvedValue({
        ok: true,
        data: mockStats,
        error: undefined
      });
    });

    test('sets up auto-refresh interval', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(mockApiService.getLocations).toHaveBeenCalledTimes(1);
        expect(mockApiService.getDashboardStats).toHaveBeenCalledTimes(1);
      });

      // Fast forward 30 seconds
      jest.advanceTimersByTime(30000);

      await waitFor(() => {
        expect(mockApiService.getLocations).toHaveBeenCalledTimes(2);
        expect(mockApiService.getDashboardStats).toHaveBeenCalledTimes(2);
      });
    });

    test('clears interval on component unmount', async () => {
      const clearIntervalSpy = jest.spyOn(global, 'clearInterval');
      
      const { unmount } = render(<Dashboard />);

      await waitFor(() => {
        expect(mockApiService.getLocations).toHaveBeenCalledTimes(1);
      });

      unmount();

      expect(clearIntervalSpy).toHaveBeenCalled();
      clearIntervalSpy.mockRestore();
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      mockApiService.getLocations.mockResolvedValue({
        ok: true,
        data: mockLocations,
        error: undefined
      });
      mockApiService.getDashboardStats.mockResolvedValue({
        ok: true,
        data: mockStats,
        error: undefined
      });
    });

    test('has proper ARIA labels and structure', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByRole('table')).toHaveAttribute('aria-labelledby', 'locations-table-title');
      });

      expect(screen.getByRole('region', { name: /dashboard statistics/i })).toBeInTheDocument();
      
      const statsCards = screen.getAllByRole('article');
      expect(statsCards).toHaveLength(4);
    });

    test('status icons have proper aria-hidden attributes', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        const icons = screen.getAllByRole('img', { hidden: true });
        icons.forEach(icon => {
          expect(icon).toHaveAttribute('aria-hidden', 'true');
        });
      });
    });

    test('action buttons have descriptive aria-labels', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /manage downtown restaurant location/i })).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /delete downtown restaurant location/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /visit downtown restaurant location/i })).toBeInTheDocument();
    });

    test('alert has proper ARIA attributes', async () => {
      render(<Dashboard />);

      await waitFor(() => {
        const alert = screen.getByRole('alert');
        expect(alert).toHaveAttribute('aria-live', 'polite');
      });
    });
  });
});