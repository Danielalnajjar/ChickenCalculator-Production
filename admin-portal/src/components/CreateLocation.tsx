import React, { useState } from 'react';
import { BuildingStorefrontIcon, GlobeAltIcon, MapPinIcon } from '@heroicons/react/24/outline';

interface LocationFormData {
  name: string;
  domain: string;
  address: string;
  managerName: string;
  managerEmail: string;
  cloudProvider: 'aws' | 'digitalocean' | 'local';
  region: string;
}

const CreateLocation: React.FC = () => {
  const [formData, setFormData] = useState<LocationFormData>({
    name: '',
    domain: '',
    address: '',
    managerName: '',
    managerEmail: '',
    cloudProvider: 'digitalocean',
    region: 'nyc3',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [deploymentStatus, setDeploymentStatus] = useState<string | null>(null);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setDeploymentStatus('Creating location...');

    try {
      const response = await fetch('/api/admin/locations', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        const result = await response.json();
        setDeploymentStatus(`✅ Location "${formData.name}" created successfully!`);
        
        // Reset form
        setTimeout(() => {
          setFormData({
            name: '',
            domain: '',
            address: '',
            managerName: '',
            managerEmail: '',
            cloudProvider: 'digitalocean',
            region: 'nyc3',
          });
          setDeploymentStatus(null);
        }, 3000);
      } else {
        setDeploymentStatus('❌ Failed to create location. Please try again.');
      }
    } catch (error) {
      setDeploymentStatus('❌ Network error. Please check your connection.');
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
            Deploy a new Chicken Calculator instance for a new location
          </p>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Location Details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                placeholder="e.g., Downtown, Airport Mall"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Domain Name *
              </label>
              <div className="flex">
                <input
                  type="text"
                  name="domain"
                  value={formData.domain}
                  onChange={handleInputChange}
                  required
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="downtown"
                />
                <span className="px-3 py-2 bg-gray-100 border border-l-0 border-gray-300 rounded-r-md text-sm text-gray-600">
                  .yourcompany.com
                </span>
              </div>
            </div>
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

          {/* Deployment Configuration */}
          <div className="border-t pt-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              <GlobeAltIcon className="h-5 w-5 inline mr-2" />
              Deployment Configuration
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Cloud Provider
                </label>
                <select
                  name="cloudProvider"
                  value={formData.cloudProvider}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="digitalocean">DigitalOcean ($20/month)</option>
                  <option value="aws">AWS ECS ($35/month)</option>
                  <option value="local">Local Server ($0/month)</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Region
                </label>
                <select
                  name="region"
                  value={formData.region}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {formData.cloudProvider === 'digitalocean' && (
                    <>
                      <option value="nyc3">New York 3</option>
                      <option value="sfo3">San Francisco 3</option>
                      <option value="lon1">London 1</option>
                    </>
                  )}
                  {formData.cloudProvider === 'aws' && (
                    <>
                      <option value="us-east-1">US East (Virginia)</option>
                      <option value="us-west-2">US West (Oregon)</option>
                      <option value="eu-west-1">Europe (Ireland)</option>
                    </>
                  )}
                  {formData.cloudProvider === 'local' && (
                    <option value="local">Local Network</option>
                  )}
                </select>
              </div>
            </div>
          </div>

          {/* Status Display */}
          {deploymentStatus && (
            <div className={`p-4 rounded-md ${deploymentStatus.includes('✅') ? 'bg-green-50 text-green-800' : deploymentStatus.includes('❌') ? 'bg-red-50 text-red-800' : 'bg-blue-50 text-blue-800'}`}>
              {deploymentStatus}
            </div>
          )}

          {/* Submit Button */}
          <div className="flex justify-end space-x-4">
            <button
              type="button"
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
        <h3 className="text-lg font-medium text-blue-900 mb-2">What happens next?</h3>
        <ul className="text-sm text-blue-800 space-y-2">
          <li>• 🚀 Server will be provisioned automatically</li>
          <li>• 🐳 Docker containers will be deployed</li>
          <li>• 🗄️ Database will be created with isolation</li>
          <li>• 🔐 SSL certificate will be generated</li>
          <li>• 📧 Manager will receive login credentials via email</li>
          <li>• ⏱️ Typical deployment time: 5-10 minutes</li>
        </ul>
      </div>
    </div>
  );
};

export default CreateLocation;