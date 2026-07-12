/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Connors RSI (Larry Connors). Promedio de 3 componentes de muy corto plazo:
 * 1) RSI(3) clásico sobre el cierre.
 * 2) RSI(2) aplicado a la longitud de la racha alcista/bajista consecutiva.
 * 3) Percentil (rango 0-100) del Rate-of-Change de 1 barra dentro de las últimas 100 barras.
 * Fuente: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/connorsrsi
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (uso de RSICalculator,
 * ver SQ/Calculators/RSICalculator.java).
 */
package SQ.Blocks.Indicators.ConnorsRSI;

import SQ.Calculators.RSICalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CRSI) Connors RSI", display="ConnorsRSI(@Chart@#RSIPeriod#,#StreakPeriod#,#RankPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Connors RSI (Larry Connors). Promedio de RSI(3) + RSI(2) de racha + percentil de ROC(1). Diseñado para reversión de muy corto plazo. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=1)
@ParameterSet(set="RSIPeriod=3,StreakPeriod=2,RankPeriod=100")
public class ConnorsRSI extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="RSIPeriod", minValue=2, maxValue=50, defaultValue="3", step=1)
	public int RSIPeriod;

	@Parameter(category="Default", name="StreakPeriod", minValue=2, maxValue=50, defaultValue="2", step=1)
	public int StreakPeriod;

	@Parameter(category="Default", name="RankPeriod", minValue=10, maxValue=250, defaultValue="100", step=1)
	public int RankPeriod;

	@Output(name = "CRSI", color = Colors.Red)
	public DataSeries Value;

	private RSICalculator rsiClose;
	private RSICalculator rsiStreak;
	private int currentStreak = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		rsiClose = new RSICalculator(RSIPeriod);
		rsiStreak = new RSICalculator(StreakPeriod);
		currentStreak = 0;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);

		rsiClose.onBarUpdate(close, getCurrentBar());
		double rsi3 = rsiClose.getValue();

		if (getCurrentBar() == 0) {
			Value.set(0, 50);
			currentStreak = 0;
			return;
		}

		double prevClose = Chart.Close.get(1);

		if (close > prevClose) {
			currentStreak = (currentStreak > 0) ? currentStreak + 1 : 1;
		} else if (close < prevClose) {
			currentStreak = (currentStreak < 0) ? currentStreak - 1 : -1;
		} else {
			currentStreak = 0;
		}

		rsiStreak.onBarUpdate(currentStreak, getCurrentBar());
		double streakRSI = rsiStreak.getValue();

		int window = Math.min(getCurrentBar(), RankPeriod);
		double roc1 = (prevClose != 0) ? (close - prevClose) / prevClose : 0;

		int countLower = 0;
		for (int idx = 1; idx <= window; idx++) {
			double c1 = Chart.Close.get(idx);
			double c2 = (idx + 1 <= getCurrentBar()) ? Chart.Close.get(idx + 1) : c1;
			double rocPast = (c2 != 0) ? (c1 - c2) / c2 : 0;
			if (rocPast < roc1) countLower++;
		}

		double percentRank = (window > 0) ? (100.0 * countLower / window) : 50;

		double crsi = (rsi3 + streakRSI + percentRank) / 3.0;
		Value.set(0, crsi);
	}
}
