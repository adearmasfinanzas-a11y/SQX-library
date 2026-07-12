/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Elder's Force Index (Alexander Elder). (Cierre actual - Cierre anterior) x Volumen,
 * suavizado con EMA(13). Combina direccion/magnitud del cambio de precio con el
 * volumen que lo acompaño.
 * Fuente: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/force-index
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator EMA,
 * acceso a Chart.Volume.get(idx) como en AvgVolume.java).
 */
package SQ.Blocks.Indicators.ElderForceIndex;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(EFI) Elder's Force Index", display="ElderForceIndex(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Elder's Force Index. (Cierre-CierreAnterior) x Volumen, suavizado con EMA. Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="Period=13")
public class ElderForceIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="13", step=1)
	public int Period;

	@Output(name = "EFI", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator emaCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		emaCalc = new AverageCalculator(AverageCalculator.EMA, Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);
		double prevClose = (getCurrentBar() > 0) ? Chart.Close.get(1) : close;
		double volume = Chart.Volume.get(0);

		double rawForce = (close - prevClose) * volume;

		emaCalc.onBarUpdate(rawForce, getCurrentBar());
		Value.set(0, emaCalc.getValue());
	}
}
