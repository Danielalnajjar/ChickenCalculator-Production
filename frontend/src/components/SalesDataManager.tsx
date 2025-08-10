import React, { useState, useEffect } from 'react';
import { salesDataApi } from '../services/api';
import { SalesData, SalesTotals } from '../types';

const SalesDataManager: React.FC = () => {
  const [salesData, setSalesData] = useState<SalesData[]>([]);
  const [totals, setTotals] = useState<SalesTotals | null>(null);
  const [newEntry, setNewEntry] = useState<SalesData>({
    date: new Date().toISOString().split('T')[0],
    totalSales: 0,
    portionsSoy: 0,
    portionsTeriyaki: 0,
    portionsTurmeric: 0,
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [dataResponse, totalsResponse] = await Promise.all([
        salesDataApi.getAll(),
        salesDataApi.getTotals(),
      ]);
      setSalesData(dataResponse);
      setTotals(totalsResponse);
    } catch (err) {
      setError('Error loading sales data');
      console.error(err);
    }
  };

  const handleAddEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await salesDataApi.add(newEntry);
      setNewEntry({
        date: new Date().toISOString().split('T')[0],
        totalSales: 0,
        portionsSoy: 0,
        portionsTeriyaki: 0,
        portionsTurmeric: 0,
      });
      setSuccess('Sales data added successfully');
      loadData();
    } catch (err) {
      setError('Error adding sales data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteEntry = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this entry?')) {
      return;
    }

    try {
      await salesDataApi.delete(id);
      setSuccess('Entry deleted successfully');
      loadData();
    } catch (err) {
      setError('Error deleting entry');
      console.error(err);
    }
  };

  const handleDeleteAll = async () => {
    if (!window.confirm('Are you sure you want to delete ALL sales data? This cannot be undone.')) {
      return;
    }

    try {
      await salesDataApi.deleteAll();
      setSuccess('All sales data deleted');
      loadData();
    } catch (err) {
      setError('Error deleting all data');
      console.error(err);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  return (
    <div>
      <div className="form-container">
        <h1>Sales Data Management</h1>
        <p>Add historical sales data to enable marination calculations. This data is used to calculate portion ratios per $1000 in sales.</p>

        {error && (
          <div className="alert error">
            {error}
          </div>
        )}

        {success && (
          <div className="alert info">
            {success}
          </div>
        )}

        <form onSubmit={handleAddEntry}>
          <div className="form-section">
            <h2 className="section-title">Add New Sales Entry</h2>
            <div className="form-row">
              <div className="form-group">
                <label>Date</label>
                <input
                  type="date"
                  required
                  value={newEntry.date}
                  onChange={(e) => setNewEntry({
                    ...newEntry,
                    date: e.target.value
                  })}
                />
              </div>
              <div className="form-group">
                <label>Total Sales ($)</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  required
                  value={newEntry.totalSales}
                  onChange={(e) => setNewEntry({
                    ...newEntry,
                    totalSales: parseFloat(e.target.value) || 0
                  })}
                />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Soy Portions Sold</label>
                <input
                  type="number"
                  min="0"
                  required
                  value={newEntry.portionsSoy}
                  onChange={(e) => setNewEntry({
                    ...newEntry,
                    portionsSoy: parseFloat(e.target.value) || 0
                  })}
                />
              </div>
              <div className="form-group">
                <label>Teriyaki Portions Sold</label>
                <input
                  type="number"
                  min="0"
                  required
                  value={newEntry.portionsTeriyaki}
                  onChange={(e) => setNewEntry({
                    ...newEntry,
                    portionsTeriyaki: parseFloat(e.target.value) || 0
                  })}
                />
              </div>
              <div className="form-group">
                <label>Turmeric Portions Sold</label>
                <input
                  type="number"
                  min="0"
                  required
                  value={newEntry.portionsTurmeric}
                  onChange={(e) => setNewEntry({
                    ...newEntry,
                    portionsTurmeric: parseFloat(e.target.value) || 0
                  })}
                />
              </div>
            </div>
          </div>
          <button type="submit" disabled={loading}>
            {loading ? 'Adding...' : 'Add Entry'}
          </button>
        </form>
      </div>

      {totals && (
        <div className="results-container">
          <h2>Sales Summary</h2>
          <div className="results-grid">
            <div className="result-card">
              <h3>Total Sales</h3>
              <div className="result-value">
                {formatCurrency(totals.totalSales)}
              </div>
            </div>
            <div className="result-card">
              <h3>Total Soy Portions</h3>
              <div className="result-value">
                {totals.totalPortionsSoy.toLocaleString()}
              </div>
            </div>
            <div className="result-card">
              <h3>Total Teriyaki Portions</h3>
              <div className="result-value">
                {totals.totalPortionsTeriyaki.toLocaleString()}
              </div>
            </div>
            <div className="result-card">
              <h3>Total Turmeric Portions</h3>
              <div className="result-value">
                {totals.totalPortionsTurmeric.toLocaleString()}
              </div>
            </div>
          </div>

          {totals.totalSales > 0 && (
            <div className="alert info">
              <strong>Portions per $1000 sales:</strong>
              <br />
              Soy: {((totals.totalPortionsSoy / totals.totalSales) * 1000).toFixed(1)} | 
              Teriyaki: {((totals.totalPortionsTeriyaki / totals.totalSales) * 1000).toFixed(1)} | 
              Turmeric: {((totals.totalPortionsTurmeric / totals.totalSales) * 1000).toFixed(1)}
            </div>
          )}
        </div>
      )}

      <div className="table-container">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2>Historical Sales Data</h2>
          {salesData.length > 0 && (
            <button 
              onClick={handleDeleteAll} 
              className="danger"
              style={{ marginLeft: 'auto' }}
            >
              Delete All Data
            </button>
          )}
        </div>

        {salesData.length === 0 ? (
          <div className="alert info">
            No sales data found. Add some historical data to enable marination calculations.
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Total Sales</th>
                <th>Soy Portions</th>
                <th>Teriyaki Portions</th>
                <th>Turmeric Portions</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {salesData.map((entry) => (
                <tr key={entry.id}>
                  <td>{formatDate(entry.date)}</td>
                  <td>{formatCurrency(entry.totalSales)}</td>
                  <td>{entry.portionsSoy}</td>
                  <td>{entry.portionsTeriyaki}</td>
                  <td>{entry.portionsTurmeric}</td>
                  <td>
                    <button 
                      onClick={() => handleDeleteEntry(entry.id!)}
                      className="danger"
                      style={{ padding: '5px 10px', fontSize: '0.9rem' }}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default SalesDataManager;