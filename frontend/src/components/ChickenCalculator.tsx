import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { calculatorApi } from '../services/api';
import { InventoryData, ProjectedSales, CalculationResult, MarinationRequest } from '../types';

interface ChickenCalculatorProps {}

const ChickenCalculator: React.FC<ChickenCalculatorProps> = React.memo(() => {
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

  const checkSalesData = useCallback(async () => {
    try {
      const hasData = await calculatorApi.hasSalesData();
      setHasSalesData(hasData);
    } catch (err) {
      console.error('Error checking sales data:', err);
    }
  }, []);

  const handleCalculate = useCallback(async () => {
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
  }, [hasSalesData, inventory, projectedSales, useAvailableChicken, availableRawChickenKg]);

  const formatToKg = useCallback((grams: number): string => {
    return (grams / 1000).toFixed(1);
  }, []);

  const formatToPans = useCallback((grams: number, gramsPerPan: number): string => {
    const yieldFactors = { soy: 0.73, teriyaki: 0.88, turmeric: 0.86 };
    const pansPerType = { soy: 3000, teriyaki: 3200, turmeric: 1500 };
    
    // This is a simplified calculation - in real implementation, we'd pass the type
    return (grams / gramsPerPan).toFixed(1);
  }, []);

  return (
    <div>
      <div className="form-container">
        <h1 id="calculator-title">Chicken Marination Calculator</h1>
        
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
          <h2 className="section-title" id="inventory-section">Current Inventory (Pans)</h2>
          <div className="form-row" role="group" aria-labelledby="inventory-section">
            <div className="form-group">
              <label htmlFor="soy-pans">Soy Pans</label>
              <input
                id="soy-pans"
                type="number"
                min="0"
                step="0.1"
                value={inventory.pansSoy}
                onChange={(e) => setInventory({
                  ...inventory,
                  pansSoy: parseFloat(e.target.value) || 0
                })}
                aria-describedby="soy-pans-help"
                aria-label="Number of soy chicken pans in current inventory"
              />
              <div id="soy-pans-help" className="sr-only">
                Enter the number of soy chicken pans currently in inventory
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="teriyaki-pans">Teriyaki Pans</label>
              <input
                id="teriyaki-pans"
                type="number"
                min="0"
                step="0.1"
                value={inventory.pansTeriyaki}
                onChange={(e) => setInventory({
                  ...inventory,
                  pansTeriyaki: parseFloat(e.target.value) || 0
                })}
                aria-describedby="teriyaki-pans-help"
                aria-label="Number of teriyaki chicken pans in current inventory"
              />
              <div id="teriyaki-pans-help" className="sr-only">
                Enter the number of teriyaki chicken pans currently in inventory
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="turmeric-pans">Turmeric Pans</label>
              <input
                id="turmeric-pans"
                type="number"
                min="0"
                step="0.1"
                value={inventory.pansTurmeric}
                onChange={(e) => setInventory({
                  ...inventory,
                  pansTurmeric: parseFloat(e.target.value) || 0
                })}
                aria-describedby="turmeric-pans-help"
                aria-label="Number of turmeric chicken pans in current inventory"
              />
              <div id="turmeric-pans-help" className="sr-only">
                Enter the number of turmeric chicken pans currently in inventory
              </div>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h2 className="section-title" id="sales-section">Projected Sales (Next 4 Days)</h2>
          <div className="form-row" role="group" aria-labelledby="sales-section">
            <div className="form-group">
              <label htmlFor="day0-sales">Day 0 (Today) Sales ($)</label>
              <input
                id="day0-sales"
                type="number"
                min="0"
                value={projectedSales.day0}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day0: parseFloat(e.target.value) || 0
                })}
                aria-describedby="day0-sales-help"
                aria-label="Projected sales amount for today in dollars"
              />
              <div id="day0-sales-help" className="sr-only">
                Enter projected sales amount for today in dollars
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="day1-sales">Day 1 Sales ($)</label>
              <input
                id="day1-sales"
                type="number"
                min="0"
                value={projectedSales.day1}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day1: parseFloat(e.target.value) || 0
                })}
                aria-describedby="day1-sales-help"
                aria-label="Projected sales amount for day 1 in dollars"
              />
              <div id="day1-sales-help" className="sr-only">
                Enter projected sales amount for day 1 in dollars
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="day2-sales">Day 2 Sales ($)</label>
              <input
                id="day2-sales"
                type="number"
                min="0"
                value={projectedSales.day2}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day2: parseFloat(e.target.value) || 0
                })}
                aria-describedby="day2-sales-help"
                aria-label="Projected sales amount for day 2 in dollars"
              />
              <div id="day2-sales-help" className="sr-only">
                Enter projected sales amount for day 2 in dollars
              </div>
            </div>
            <div className="form-group">
              <label htmlFor="day3-sales">Day 3 Sales ($)</label>
              <input
                id="day3-sales"
                type="number"
                min="0"
                value={projectedSales.day3}
                onChange={(e) => setProjectedSales({
                  ...projectedSales,
                  day3: parseFloat(e.target.value) || 0
                })}
                aria-describedby="day3-sales-help"
                aria-label="Projected sales amount for day 3 in dollars"
              />
              <div id="day3-sales-help" className="sr-only">
                Enter projected sales amount for day 3 in dollars
              </div>
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-group">
            <label htmlFor="use-available-chicken" className="checkbox-label">
              <input
                id="use-available-chicken"
                type="checkbox"
                checked={useAvailableChicken}
                onChange={(e) => setUseAvailableChicken(e.target.checked)}
                style={{ marginRight: '10px' }}
                aria-describedby="use-available-chicken-help"
              />
              Limit by available raw chicken
            </label>
            <div id="use-available-chicken-help" className="sr-only">
              Check this box to limit calculations based on available raw chicken inventory
            </div>
          </div>
          
          {useAvailableChicken && (
            <div className="form-group">
              <label htmlFor="available-chicken-kg">Available Raw Chicken (kg)</label>
              <input
                id="available-chicken-kg"
                type="number"
                min="0"
                step="0.1"
                value={availableRawChickenKg}
                onChange={(e) => setAvailableRawChickenKg(parseFloat(e.target.value) || 0)}
                aria-describedby="available-chicken-help"
                aria-label="Amount of available raw chicken in kilograms"
              />
              <div id="available-chicken-help" className="sr-only">
                Enter the amount of raw chicken available for marination in kilograms
              </div>
            </div>
          )}
        </div>

        <button 
          onClick={handleCalculate} 
          disabled={loading || !hasSalesData}
          aria-describedby="calculate-button-help"
          aria-label={loading ? 'Calculating marination requirements' : 'Calculate marination requirements based on entered data'}
        >
          {loading ? 'Calculating...' : 'Calculate Marination'}
        </button>
        <div id="calculate-button-help" className="sr-only">
          Click to calculate how much chicken to marinate based on inventory and projected sales
        </div>
      </div>

      {result && (
        <div className="results-container" role="region" aria-labelledby="results-title">
          <h2 id="results-title">Marination Results</h2>
          <div className="results-grid" role="group" aria-labelledby="results-title">
            <div className="result-card" role="article" aria-labelledby="soy-result">
              <h3 id="soy-result">Soy Chicken</h3>
              <div className="result-value" aria-label={`${formatToKg(result.rawToMarinateSoy)} kilograms of soy chicken to marinate`}>
                {formatToKg(result.rawToMarinateSoy)} <span className="result-unit">kg</span>
              </div>
              <p aria-label={`Approximately ${formatToPans(result.rawToMarinateSoy, 3000 / 0.73)} pans`}>
                ({formatToPans(result.rawToMarinateSoy, 3000 / 0.73)} pans approx.)
              </p>
            </div>
            
            <div className="result-card" role="article" aria-labelledby="teriyaki-result">
              <h3 id="teriyaki-result">Teriyaki Chicken</h3>
              <div className="result-value" aria-label={`${formatToKg(result.rawToMarinateTeriyaki)} kilograms of teriyaki chicken to marinate`}>
                {formatToKg(result.rawToMarinateTeriyaki)} <span className="result-unit">kg</span>
              </div>
              <p aria-label={`Approximately ${formatToPans(result.rawToMarinateTeriyaki, 3200 / 0.88)} pans`}>
                ({formatToPans(result.rawToMarinateTeriyaki, 3200 / 0.88)} pans approx.)
              </p>
            </div>
            
            <div className="result-card" role="article" aria-labelledby="turmeric-result">
              <h3 id="turmeric-result">Turmeric Chicken</h3>
              <div className="result-value" aria-label={`${formatToKg(result.rawToMarinateTurmeric)} kilograms of turmeric chicken to marinate`}>
                {formatToKg(result.rawToMarinateTurmeric)} <span className="result-unit">kg</span>
              </div>
              <p aria-label={`Approximately ${formatToPans(result.rawToMarinateTurmeric, 1500 / 0.86)} pans`}>
                ({formatToPans(result.rawToMarinateTurmeric, 1500 / 0.86)} pans approx.)
              </p>
            </div>
          </div>

          <div className="alert info" role="complementary" aria-labelledby="historical-ratios">
            <strong id="historical-ratios">Historical Ratios (per $1000 sales):</strong>
            <br />
            <span aria-label={`Soy: ${result.portionsPer1000Soy.toFixed(1)} portions per 1000 dollars in sales`}>
              Soy: {result.portionsPer1000Soy.toFixed(1)} portions
            </span> | 
            <span aria-label={`Teriyaki: ${result.portionsPer1000Teriyaki.toFixed(1)} portions per 1000 dollars in sales`}>
              Teriyaki: {result.portionsPer1000Teriyaki.toFixed(1)} portions
            </span> | 
            <span aria-label={`Turmeric: ${result.portionsPer1000Turmeric.toFixed(1)} portions per 1000 dollars in sales`}>
              Turmeric: {result.portionsPer1000Turmeric.toFixed(1)} portions
            </span>
          </div>
        </div>
      )}
    </div>
  );
});

ChickenCalculator.displayName = 'ChickenCalculator';

export default ChickenCalculator;