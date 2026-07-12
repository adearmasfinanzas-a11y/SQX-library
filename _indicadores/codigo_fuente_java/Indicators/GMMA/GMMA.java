/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * GMMA - Guppy Multiple Moving Average (Daryl Guppy). Dos grupos de EMA (corto:
 * 3,5,8,10,12,15 / largo: 30,35,40,45,50,60) que representan traders de corto y
 * largo plazo. La separación entre el promedio de ambos grupos mide la fuerza de
 * la tendencia: separación amplia = tendencia fuerte, angosta/entrelazada = consolidación.
 * Se implementa como el promedio de cada grupo + la separación porcentual entre
 * ambos, en vez de exponer las 12 EMA individuales (impracticable como bloque).
 * Fuente: https://www.litefinance.org/blog/for-beginners/best-technical-indicators/guppy-multiple-moving-average/
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator EMA
 * multiple instancias, como en MACD.java).
 */
package SQ.Blocks.Indicators.GMMA;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(GMMA) Guppy Multiple Moving Average", display="GMMA(@Chart@)[#Shift#]", returnType = ReturnTypes.Number)
@Help("GMMA (Guppy). Separacion porcentual entre el promedio del grupo corto (3-15) y el grupo largo (30-60) de EMAs. Amplia = tendencia fuerte, angosta = consolidacion. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0)
public class GMMA extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Output(name = "ShortAvg", color = Colors.Green)
	public DataSeries ShortAvg;

	@Output(name = "LongAvg", color = Colors.Red)
	public DataSeries LongAvg;

	@Output(name = "Separation", color = Colors.Blue)
	public DataSeries Separation;

	private static final int[] SHORT_PERIODS = {3, 5, 8, 10, 12, 15};
	private static final int[] LONG_PERIODS = {30, 35, 40, 45, 50, 60};

	private AverageCalculator[] shortEMAs;
	private AverageCalculator[] longEMAs;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		shortEMAs = new AverageCalculator[SHORT_PERIODS.length];
		for (int i = 0; i < SHORT_PERIODS.length; i++) {
			shortEMAs[i] = new AverageCalculator(AverageCalculator.EMA, SHORT_PERIODS[i]);
		}

		longEMAs = new AverageCalculator[LONG_PERIODS.length];
		for (int i = 0; i < LONG_PERIODS.length; i++) {
			longEMAs[i] = new AverageCalculator(AverageCalculator.EMA, LONG_PERIODS[i]);
		}
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);

		double shortSum = 0;
		for (AverageCalculator calc : shortEMAs) {
			calc.onBarUpdate(close, getCurrentBar());
			shortSum += calc.getValue();
		}
		double shortAvg = shortSum / shortEMAs.length;

		double longSum = 0;
		for (AverageCalculator calc : longEMAs) {
			calc.onBarUpdate(close, getCurrentBar());
			longSum += calc.getValue();
		}
		double longAvg = longSum / longEMAs.length;

		double separation = (longAvg != 0) ? 100.0 * (shortAvg - longAvg) / longAvg : 0;

		ShortAvg.set(0, shortAvg);
		LongAvg.set(0, longAvg);
		Separation.set(0, separation);
	}
}
