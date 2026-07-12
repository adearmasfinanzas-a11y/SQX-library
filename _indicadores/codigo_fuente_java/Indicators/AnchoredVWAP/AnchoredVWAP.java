/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Anchored VWAP. El VWAP nativo es de sesión (se reinicia periódicamente); un VWAP
 * "anclado" clásico se fija manualmente a un evento significativo (swing point,
 * apertura de sesión, fecha de noticia) y acumula desde ahí sin reiniciarse — un
 * anclaje manual no es automatizable como building block determinista, así que acá
 * se define un ANCLAJE AUTOMÁTICO: el más reciente entre el máximo o el mínimo
 * (swing point) dentro de una ventana de lookback, usando HighestCalculator/
 * LowestCalculator.getHighestIndex()/getLowestIndex() para ubicarlo.
 * Fuente: concepto estándar de Anchored VWAP, adaptado a una regla automatizable
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (HighestCalculator/
 * LowestCalculator, acceso histórico Chart.X.get(idx)).
 */
package SQ.Blocks.Indicators.AnchoredVWAP;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(AVWAP) Anchored VWAP", display="AnchoredVWAP(@Chart@#LookbackPeriod#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Anchored VWAP. Se ancla automaticamente al swing point (maximo o minimo) mas reciente dentro de la ventana de lookback, y acumula VWAP desde ahi sin reiniciarse. Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@ParameterSet(set="LookbackPeriod=50")
public class AnchoredVWAP extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="LookbackPeriod", minValue=5, maxValue=250, defaultValue="50", step=1)
	public int LookbackPeriod;

	@Output(name = "AVWAP", color = Colors.Red)
	public DataSeries Value;

	private HighestCalculator highestCalc;
	private LowestCalculator lowestCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestCalc = new HighestCalculator(LookbackPeriod);
		lowestCalc = new LowestCalculator(LookbackPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		highestCalc.onBarUpdate(Chart.High.get(0), getCurrentBar());
		lowestCalc.onBarUpdate(Chart.Low.get(0), getCurrentBar());

		int anchorBarsAgo = Math.min(highestCalc.getHighestIndex(), lowestCalc.getLowestIndex());
		anchorBarsAgo = Math.min(anchorBarsAgo, getCurrentBar());

		double sumPV = 0, sumV = 0;
		for (int idx = 0; idx <= anchorBarsAgo; idx++) {
			double typical = (Chart.High.get(idx) + Chart.Low.get(idx) + Chart.Close.get(idx)) / 3.0;
			double v = Chart.Volume.get(idx);
			if (v < 0.0000000001) v = 1;
			sumPV += typical * v;
			sumV += v;
		}

		double avwap = (sumV < 0.0000000001) ? Chart.Close.get(0) : sumPV / sumV;
		Value.set(0, avwap);
	}
}
