/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Bollinger Band Width (%BW). (BandaSuperior - BandaInferior) / BandaMedia * 100.
 * Bollinger Bands es nativo, pero el ANCHO normalizado como valor propio (usado
 * clásicamente para detectar "squeeze" — contracción de volatilidad previa a
 * ruptura) no está expuesto como indicador standalone.
 * Fuente: concepto estándar derivado de Bollinger Bands (John Bollinger)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator SMA
 * + StdDevCalculator).
 */
package SQ.Blocks.Indicators.BollingerBandWidth;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(BBW) Bollinger Band Width", display="BollingerBandWidth(@Chart@#Period#,#Deviation#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Bollinger Band Width. Ancho normalizado de las Bandas de Bollinger, usado clasicamente para detectar contraccion de volatilidad (squeeze) previa a ruptura. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0.1, min=0, max=1, step=0.001)
@ParameterSet(set="Period=20,Deviation=2")
public class BollingerBandWidth extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=250, defaultValue="20", step=1)
	public int Period;

	@Parameter(category="Default", name="Deviation", minValue=0.5, maxValue=5, defaultValue="2", step=0.1)
	public double Deviation;

	@Output(name = "BBW", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator smaCalc;
	private StdDevCalculator stdDevCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		smaCalc = new AverageCalculator(AverageCalculator.SMA, Period);
		stdDevCalc = new StdDevCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);

		smaCalc.onBarUpdate(close, getCurrentBar());
		stdDevCalc.onBarUpdate(close, getCurrentBar());

		double middle = smaCalc.getValue();
		double stdDev = stdDevCalc.getValue();

		double upper = middle + Deviation * stdDev;
		double lower = middle - Deviation * stdDev;

		double width = (Math.abs(middle) < 0.0000000001) ? 0 : (upper - lower) / middle;

		Value.set(0, width);
	}
}
