import React, { useState, useEffect } from 'react';
import { calculatorApi } from '../services/api';
import { InventoryData, ProjectedSales, CalculationResult, MarinationRequest } from '../types';

const ChickenCalculator: React.FC = () => {
  const [inventory, setInventory] = useState<InventoryData>({
    pansSoy: 0,
    pansTeriyaki: 0,
    pansTurmeric: 0,
  });

  const [projectedSales, setProjectedSales] = useState<ProjectedSales>({
    day0: 0,
    day1: 0,
    day2: 0,
    day3: 0,
  });

  const [availableRawChickenKg, setAvailableRawChickenKg] = useState<number>(0);
  const [useAvailableChicken, setUseAvailableChicken] = useState<boolean>(false);

  const [result, setResult] = useState<CalculationResult | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [hasSalesData, setHasSalesData] = useState<boolean>(false);

  useEffect(() => {
    checkSalesData();
  }, []);

  const checkSalesData = async () => {
    try {
      const hasData = await calculatorApi.hasSalesData();
      setHasSalesData(hasData);
    } catch (err) {
      console.error('Error checking sales data:', err);
    }
  };

  const handleCalculate = async () => {
    if (!hasSalesData) {
      setError('Please add historical sales data first in the Sales Data section.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const request: MarinationRequest = {
        inventory,
        projectedSales,
        ...(useAvailableChicken && { availableRawChickenKg }),
      };

      const calculationResult = await calculatorApi.calculate(request);
      setResult(calculationResult);
    } catch (err) {
      setError('Error calculating marination. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatToKg = (grams: number): string => {
    return (grams / 1000).toFixed(1);
  };

  const formatToPans = (grams: number, gramsPerPan: number): string => {
    const yieldFactors = { soy: 0.73, teriyaki: 0.88, turmeric: 0.86 };
    const pansPerType = { soy: 3000, teriyaki: 3200, turmeric: 1500 };
    
    // This is a simplified calculation - in real implementation, we'd pass the type
    return (grams / gramsPerPan).toFixed(1);
  };

  return (
    <div>
      <div className="form-container">
        <h1>Chicken Marination Calculator</h1>
        
        {!hasSalesData && (
          <div className="alert warning">
            <strong>Warning:</strong> No historical sales data found. Please add sales data first to enable calculations.
          </div>
        )}

        {error && (
          <div className="alert error">
            {error}
          </div>
        )}

        <div className="form-section">
          <h2 className="section-title">Current Inventory (Pans)</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Soy Pans</label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={inventory.pansSoy}
                onChange={(e) => setInventory({
                  ...inventory,
                  pansSoy: parseFloat(e.target.value) || 0
                })}
              />
            </div>
            <div className="form-group">
              <label>Teriyaki Pans</label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={inventory.pansTeriyaki}
                onChange={(e) => setInventory({
                  ...inventory,
                  pansTeriyaki: parseFloat(e.target.value) || 0
                })}
              />
            </div>
            <div className="form-group">
              <label>Turmeric Pans</label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={inventory.pansTurmeric}
                onChange={(e) => setInventory({
                  ...inventory,
                  pansTurmeric: parseFloat(e.target.value) || 0
                })}
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h2 className="section-title">Projected Sales (Next 4 Days)</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Day 0 (Today) Sales ($)</label>
              <input
                type="number"
                min="0"
                value={projectedSales.day0}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day0: parseFloat(e.target.value) || 0
                })}
              />
            </div>
            <div className="form-group">
              <label>Day 1 Sales ($)</label>
              <input
                type="number"
                min="0"
                value={projectedSales.day1}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day1: parseFloat(e.target.value) || 0
                })}
              />
            </div>
            <div className="form-group">
              <label>Day 2 Sales ($)</label>
              <input
                type="number"
                min="0"
                value={projectedSales.day2}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day2: parseFloat(e.target.value) || 0
                })}
              />
            </div>
            <div className="form-group">
              <label>Day 3 Sales ($)</label>
              <input
                type="number"
                min="0"
                value={projectedSales.day3}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day3: parseFloat(e.target.value) || 0
                })}
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-group">
            <label>
              <input
                type="checkbox"
                checked={useAvailableChicken}
                onChange={(e) => setUseAvailableChicken(e.target.checked)}
                style={{ marginRight: '10px' }}
              />
              Limit by available raw chicken
            </label>
          </div>
          
          {useAvailableChicken && (
            <div className="form-group">
              <label>Available Raw Chicken (kg)</label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={availableRawChickenKg}
                onChange={(e) => setAvailableRawChickenKg(parseFloat(e.target.value) || 0)}
              />
            </div>
          )}
        </div>

        <button 
          onClick={handleCalculate} 
          disabled={loading || !hasSalesData}
        >
          {loading ? 'Calculating...' : 'Calculate Marination'}
        </button>
      </div>

      {result && (
        <div className="results-container">
          <h2>Marination Results</h2>
          <div className="results-grid">
            <div className="result-card">
              <h3>Soy Chicken</h3>
              <div className="result-value">
                {formatToKg(result.rawToMarinateSoy)} <span className="result-unit">kg</span>
              </div>
              <p>({formatToPans(result.rawToMarinateSoy, 3000 / 0.73)} pans approx.)</p>
            </div>
            
            <div className="result-card">
              <h3>Teriyaki Chicken</h3>
              <div className="result-value">
                {formatToKg(result.rawToMarinateTeriyaki)} <span className="result-unit">kg</span>
              </div>
              <p>({formatToPans(result.rawToMarinateTeriyaki, 3200 / 0.88)} pans approx.)</p>
            </div>
            
            <div className="result-card">
              <h3>Turmeric Chicken</h3>
              <div className="result-value">
                {formatToKg(result.rawToMarinateTurmeric)} <span className="result-unit">kg</span>
              </div>
              <p>({formatToPans(result.rawToMarinateTurmeric, 1500 / 0.86)} pans approx.)</p>
            </div>
          </div>

          <div className="alert info">
            <strong>Historical Ratios (per $1000 sales):</strong>
            <br />
            Soy: {result.portionsPer1000Soy.toFixed(1)} portions | 
            Teriyaki: {result.portionsPer1000Teriyaki.toFixed(1)} portions | 
            Turmeric: {result.portionsPer1000Turmeric.toFixed(1)} portions
          </div>
        </div>
      )}
    </div>
  );
};

export default ChickenCalculator;