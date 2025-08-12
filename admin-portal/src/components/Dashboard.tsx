import React, { useState, useEffect } from 'react';
import { 
  BuildingStorefrontIcon, 
  ServerIcon, 
  ChartBarIcon, 
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon
} from '@heroicons/react/24/outline';
import apiService from '../services/api';

interface Location {
  id: string;
  name: string;
  domain: string;
  status: 'active' | 'deploying' | 'error';
  lastSeen: string;
  managerEmail: string;
  cloudProvider: string;
  region: string;
  createdAt: string;
}

interface DashboardStats {
  totalLocations: number;
  activeLocations: number;
  deployingLocations: number;
  errorLocations: number;
  totalTransactions: number;
  totalRevenue: number;
}

const Dashboard: React.FC = () => {
  const [locations, setLocations] = useState<Location[]>([]);
  const [stats, setStats] = useState<DashboardStats>({
    totalLocations: 0,
    activeLocations: 0,
    deployingLocations: 0,
    errorLocations: 0,
    totalTransactions: 0,
    totalRevenue: 0,
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [locationsResponse, statsResponse] = await Promise.all([
          apiService.getLocations(),
          apiService.getDashboardStats(),
        ]);

        if (locationsResponse.ok && locationsResponse.data) {
          setLocations(locationsResponse.data as Location[]);
        }
        
        if (statsResponse.ok && statsResponse.data) {
          setStats(statsResponse.data as DashboardStats);
        }
        
        // Log any errors
        if (!locationsResponse.ok) {
          console.error('Error fetching locations:', locationsResponse.error);
        }
        if (!statsResponse.ok) {
          console.error('Error fetching stats:', statsResponse.error);
        }
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboardData();
    // Refresh every 30 seconds
    const interval = setInterval(fetchDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'active':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />;
      case 'deploying':
        return <ClockIcon className="h-5 w-5 text-yellow-500" />;
      case 'error':
        return <ExclamationTriangleIcon className="h-5 w-5 text-red-500" />;
      default:
        return <ServerIcon className="h-5 w-5 text-gray-500" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active':
        return 'bg-green-100 text-green-800';
      case 'deploying':
        return 'bg-yellow-100 text-yellow-800';
      case 'error':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900" id="dashboard-title">Dashboard</h1>
        <p className="text-gray-600">Overview of all Chicken Calculator locations</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6" role="region" aria-labelledby="stats-title">
        <h2 id="stats-title" className="sr-only">Dashboard Statistics</h2>
        
        <div className="bg-white rounded-lg shadow p-6" role="article" aria-labelledby="total-locations-stat">
          <div className="flex items-center">
            <BuildingStorefrontIcon className="h-8 w-8 text-blue-600" aria-hidden="true" />
            <div className="ml-4">
              <p className="text-2xl font-bold text-gray-900" id="total-locations-stat" aria-label={`${stats.totalLocations} total locations`}>
                {stats.totalLocations}
              </p>
              <p className="text-sm text-gray-600">Total Locations</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6" role="article" aria-labelledby="active-locations-stat">
          <div className="flex items-center">
            <CheckCircleIcon className="h-8 w-8 text-green-600" aria-hidden="true" />
            <div className="ml-4">
              <p className="text-2xl font-bold text-gray-900" id="active-locations-stat" aria-label={`${stats.activeLocations} active locations`}>
                {stats.activeLocations}
              </p>
              <p className="text-sm text-gray-600">Active</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6" role="article" aria-labelledby="total-transactions-stat">
          <div className="flex items-center">
            <ChartBarIcon className="h-8 w-8 text-purple-600" aria-hidden="true" />
            <div className="ml-4">
              <p className="text-2xl font-bold text-gray-900" id="total-transactions-stat" aria-label={`${stats.totalTransactions.toLocaleString()} total transactions`}>
                {stats.totalTransactions.toLocaleString()}
              </p>
              <p className="text-sm text-gray-600">Total Transactions</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6" role="article" aria-labelledby="total-revenue-stat">
          <div className="flex items-center">
            <ServerIcon className="h-8 w-8 text-green-600" aria-hidden="true" />
            <div className="ml-4">
              <p className="text-2xl font-bold text-gray-900" id="total-revenue-stat" aria-label={`$${stats.totalRevenue.toLocaleString()} total revenue`}>
                ${stats.totalRevenue.toLocaleString()}
              </p>
              <p className="text-sm text-gray-600">Total Revenue</p>
            </div>
          </div>
        </div>
      </div>

      {/* Alerts */}
      {stats.errorLocations > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4" role="alert" aria-live="polite">
          <div className="flex">
            <ExclamationTriangleIcon className="h-5 w-5 text-red-400" aria-hidden="true" />
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                {stats.errorLocations} location(s) need attention
              </h3>
              <p className="text-sm text-red-700 mt-1">
                Check the locations list below for deployment issues.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Locations Table */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900" id="locations-table-title">All Locations</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200" role="table" aria-labelledby="locations-table-title">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Location
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Manager
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Infrastructure
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Last Seen
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {locations.map((location) => (
                <tr key={location.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div>
                      <div className="text-sm font-medium text-gray-900">{location.name}</div>
                      <div className="text-sm text-gray-500">
                        <a 
                          href={`https://${location.domain}`} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:text-blue-800"
                        >
                          {location.domain}
                        </a>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      {getStatusIcon(location.status)}
                      <span className={`ml-2 inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(location.status)}`}>
                        {location.status}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {location.managerEmail}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {location.cloudProvider} ({location.region})
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {new Date(location.lastSeen).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex flex-col sm:flex-row gap-2">
                      <a 
                        href={`https://${location.domain}`} 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className="text-blue-600 hover:text-blue-900 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded px-2 py-1 min-h-[44px] flex items-center justify-center"
                        aria-label={`Visit ${location.name} location`}
                      >
                        Visit
                      </a>
                      <button 
                        className="text-indigo-600 hover:text-indigo-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded px-2 py-1 min-h-[44px]"
                        aria-label={`Manage ${location.name} location`}
                      >
                        Manage
                      </button>
                      <button 
                        className="text-red-600 hover:text-red-900 focus:outline-none focus:ring-2 focus:ring-red-500 rounded px-2 py-1 min-h-[44px]"
                        aria-label={`Delete ${location.name} location`}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          
          {locations.length === 0 && (
            <div className="text-center py-12" role="region" aria-labelledby="no-locations-title">
              <BuildingStorefrontIcon className="mx-auto h-12 w-12 text-gray-400" aria-hidden="true" />
              <h3 id="no-locations-title" className="mt-2 text-sm font-medium text-gray-900">No locations yet</h3>
              <p className="mt-1 text-sm text-gray-500">Get started by creating your first location.</p>
              <div className="mt-6">
                <button
                  type="button"
                  className="inline-flex items-center px-4 py-3 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 min-h-[44px]"
                  aria-label="Create your first location"
                >
                  <BuildingStorefrontIcon className="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
                  Create Location
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;