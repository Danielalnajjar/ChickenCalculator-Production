import React, { useState, useEffect } from 'react';
import { marinationLogApi } from '../services/api';
import { MarinationLog } from '../types';

const MarinationHistory: React.FC = () => {
  const [logs, setLogs] = useState<MarinationLog[]>([]);
  const [todaysLogs, setTodaysLogs] = useState<MarinationLog[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [allLogs, todaysLogsData] = await Promise.all([
        marinationLogApi.getAll(),
        marinationLogApi.getTodaysLogs(),
      ]);
      setLogs(allLogs);
      setTodaysLogs(todaysLogsData);
    } catch (err) {
      setError('Error loading marination logs');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteLog = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this log entry?')) {
      return;
    }

    try {
      await marinationLogApi.delete(id);
      loadData();
    } catch (err) {
      setError('Error deleting log entry');
      console.error(err);
    }
  };

  const formatDateTime = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatToKg = (grams: number): string => {
    return (grams / 1000).toFixed(1);
  };

  const calculateTodaysTotals = () => {
    if (todaysLogs.length === 0) return null;

    const totals = todaysLogs.reduce(
      (acc, log) => ({
        soy: acc.soy + log.soySuggested,
        teriyaki: acc.teriyaki + log.teriyakiSuggested,
        turmeric: acc.turmeric + log.turmericSuggested,
      }),
      { soy: 0, teriyaki: 0, turmeric: 0 }
    );

    return totals;
  };

  const todaysTotals = calculateTodaysTotals();

  if (loading) {
    return <div className="loading">Loading marination history...</div>;
  }

  return (
    <div>
      <div className="form-container">
        <h1>Marination History</h1>
        <p>Track marination calculations and decisions over time.</p>

        {error && (
          <div className="alert error">
            {error}
          </div>
        )}
      </div>

      {todaysTotals && (
        <div className="results-container">
          <h2>Today's Marination Summary</h2>
          <div className="results-grid">
            <div className="result-card">
              <h3>Soy Marinated Today</h3>
              <div className="result-value">
                {formatToKg(todaysTotals.soy * 1000)} <span className="result-unit">kg</span>
              </div>
            </div>
            <div className="result-card">
              <h3>Teriyaki Marinated Today</h3>
              <div className="result-value">
                {formatToKg(todaysTotals.teriyaki * 1000)} <span className="result-unit">kg</span>
              </div>
            </div>
            <div className="result-card">
              <h3>Turmeric Marinated Today</h3>
              <div className="result-value">
                {formatToKg(todaysTotals.turmeric * 1000)} <span className="result-unit">kg</span>
              </div>
            </div>
          </div>
          <div className="alert info">
            <strong>Note:</strong> This shows chicken that has been marinated today but may not yet be processed into pans.
          </div>
        </div>
      )}

      <div className="table-container">
        <h2>All Marination Logs</h2>
        
        {logs.length === 0 ? (
          <div className="alert info">
            No marination logs found. Calculations will appear here once you start using the calculator.
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Date & Time</th>
                <th>Soy (kg)</th>
                <th>Soy Pans</th>
                <th>Teriyaki (kg)</th>
                <th>Teriyaki Pans</th>
                <th>Turmeric (kg)</th>
                <th>Turmeric Pans</th>
                <th>End of Day</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{formatDateTime(log.timestamp!)}</td>
                  <td>{log.soySuggested.toFixed(1)}</td>
                  <td>{log.soyPans.toFixed(1)}</td>
                  <td>{log.teriyakiSuggested.toFixed(1)}</td>
                  <td>{log.teriyakiPans.toFixed(1)}</td>
                  <td>{log.turmericSuggested.toFixed(1)}</td>
                  <td>{log.turmericPans.toFixed(1)}</td>
                  <td>{log.isEndOfDay ? 'Yes' : 'No'}</td>
                  <td>
                    <button 
                      onClick={() => handleDeleteLog(log.id!)}
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

export default MarinationHistory;