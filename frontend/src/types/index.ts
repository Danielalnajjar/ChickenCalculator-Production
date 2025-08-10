export interface InventoryData {
  pansSoy: number;
  pansTeriyaki: number;
  pansTurmeric: number;
}

export interface ProjectedSales {
  day0: number;
  day1: number;
  day2: number;
  day3: number;
}

export interface CalculationResult {
  rawToMarinateSoy: number;
  rawToMarinateTeriyaki: number;
  rawToMarinateTurmeric: number;
  portionsPer1000Soy: number;
  portionsPer1000Teriyaki: number;
  portionsPer1000Turmeric: number;
}

export interface MarinationRequest {
  inventory: InventoryData;
  projectedSales: ProjectedSales;
  availableRawChickenKg?: number;
  alreadyMarinatedSoy?: number;
  alreadyMarinatedTeriyaki?: number;
  alreadyMarinatedTurmeric?: number;
}

export interface SalesData {
  id?: number;
  date: string;
  totalSales: number;
  portionsSoy: number;
  portionsTeriyaki: number;
  portionsTurmeric: number;
}

export interface SalesTotals {
  totalSales: number;
  totalPortionsSoy: number;
  totalPortionsTeriyaki: number;
  totalPortionsTurmeric: number;
}

export interface MarinationLog {
  id?: number;
  timestamp?: string;
  soySuggested: number;
  teriyakiSuggested: number;
  turmericSuggested: number;
  soyPans: number;
  teriyakiPans: number;
  turmericPans: number;
  isEndOfDay?: boolean;
}