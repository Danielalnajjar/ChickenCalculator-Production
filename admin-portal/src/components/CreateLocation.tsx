import React, { useState } from 'react';
import { BuildingStorefrontIcon, MapPinIcon } from '@heroicons/react/24/outline';
import apiService from '../services/api';

interface LocationFormData {
  name: string;
  address: string;
  managerName: string;
  managerEmail: string;
}

const CreateLocation: React.FC = () => {
  const [formData, setFormData] = useState<LocationFormData>({
    name: '',
    address: '',
    managerName: '',
    managerEmail: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [generatedSlug, setGeneratedSlug] = useState<string>('');

  const generateSlug = (name: string): string => {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .trim()
      .replace(/^-+|-+$/g, '');
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));

    // Auto-generate slug when name changes
    if (name === 'name') {
      setGeneratedSlug(generateSlug(value));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setStatusMessage('Creating location...');

    try {
      const response = await apiService.createLocation(formData);

      if (response.ok && response.data) {
        setStatusMessage(`✅ Location "${formData.name}" created successfully!`);
        
        // Reset form
        setTimeout(() => {
          setFormData({
            name: '',
            address: '',
            managerName: '',
            managerEmail: '',
          });
          setGeneratedSlug('');
          setStatusMessage(null);
        }, 3000);
      } else {
        setStatusMessage(`❌ Failed to create location: ${response.error || 'Unknown error'}`);
      }
    } catch (error) {
      console.error('Error creating location:', error);
      setStatusMessage(`❌ Network error: ${error instanceof Error ? error.message : 'Please check your connection'}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8">
      <div className="bg-white shadow-lg rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h1 className="text-2xl font-bold text-gray-900 flex items-center">
            <BuildingStorefrontIcon className="h-8 w-8 text-blue-600 mr-3" />
            Create New Location
          </h1>
          <p className="text-gray-600 mt-2">
            Add a new location to the Chicken Calculator system
          </p>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Location Details */}
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Location Name *
              </label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g., Downtown Store, Airport Mall"
              />
              {generatedSlug && (
                <p className="mt-1 text-sm text-gray-500">
                  URL slug: <span className="font-mono bg-gray-100 px-2 py-1 rounded">{generatedSlug}</span>
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <MapPinIcon className="h-4 w-4 inline mr-1" />
                Address
              </label>
              <input
                type="text"
                name="address"
                value={formData.address}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="123 Main St, City, State 12345"
              />
            </div>
          </div>

          {/* Manager Details */}
          <div className="border-t pt-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Location Manager</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Manager Name *
                </label>
                <input
                  type="text"
                  name="managerName"
                  value={formData.managerName}
                  onChange={handleInputChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="John Doe"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Manager Email *
                </label>
                <input
                  type="email"
                  name="managerEmail"
                  value={formData.managerEmail}
                  onChange={handleInputChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="john@yourcompany.com"
                />
              </div>
            </div>
          </div>

          {/* Status Display */}
          {statusMessage && (
            <div className={`p-4 rounded-md ${
              statusMessage.includes('✅') ? 'bg-green-50 text-green-800' : 
              statusMessage.includes('❌') ? 'bg-red-50 text-red-800' : 
              'bg-blue-50 text-blue-800'
            }`}>
              {statusMessage}
            </div>
          )}

          {/* Submit Button */}
          <div className="flex justify-end space-x-4">
            <button
              type="button"
              onClick={() => window.history.back()}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className={`px-6 py-2 text-sm font-medium text-white rounded-md ${
                isLoading
                  ? 'bg-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500'
              }`}
            >
              {isLoading ? 'Creating...' : 'Create Location'}
            </button>
          </div>
        </form>
      </div>

      {/* Info Panel */}
      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-lg font-medium text-blue-900 mb-2">How Location Management Works</h3>
        <ul className="text-sm text-blue-800 space-y-2">
          <li>• Each location has its own data segregation within the system</li>
          <li>• Managers can access their location's sales and marination data</li>
          <li>• The location slug is auto-generated from the name for easy access</li>
          <li>• All locations share the same application instance on Railway</li>
          <li>• Data is isolated between locations for security and privacy</li>
        </ul>
      </div>
    </div>
  );
};

export default CreateLocation;