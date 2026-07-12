/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Ease of Movement (Richard W. Arms Jr.). Relaciona la distancia recorrida por el
 * precio (punto medio actual vs. anterior) con el volumen necesario para moverlo,
 * ajustado por el rango de la barra. Mide que tan "facil" fue el movimiento reciente.
 * Fuente: ChartSchool (StockCharts) - ficha propia del indicador
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator SMA,
 * acceso a Chart.Volume.get(idx) como en AvgVolume.java).
 */
package SQ.Blocks.Indicators.EaseOfMovement;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(EMV) Ease of Movement", display="EaseOfMovement(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Ease of Movement (Arms). Relaciona distancia recorrida por el precio con el volumen necesario para moverlo. Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="Period=14")
public class EaseOfMovement extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="14", step=1)
	public int Period;

	@Output(name = "EMV", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator smaCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		smaCalc = new AverageCalculator(AverageCalculator.SMA, Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double h = Chart.High.get(0);
		double l = Chart.Low.get(0);
		double v = Chart.Volume.get(0);

		double midpoint = (h + l) / 2.0;
		double prevMidpoint = midpoint;
		if (getCurrentBar() > 0) {
			prevMidpoint = (Chart.High.get(1) + Chart.Low.get(1)) / 2.0;
		}

		double distance = midpoint - prevMidpoint;
		double range = h - l;
		double boxRatio = (range < 0.0000000001 || v < 0.0000000001) ? 0 : (v / 100000000.0) / range;

		double emv1 = (boxRatio < 0.0000000001) ? 0 : distance / boxRatio;

		smaCalc.onBarUpdate(emv1, getCurrentBar());
		Value.set(0, smaCalc.getValue());
	}
}
