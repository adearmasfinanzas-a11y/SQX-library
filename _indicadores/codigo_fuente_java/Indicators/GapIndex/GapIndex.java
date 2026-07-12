/*
 * Indicador propio (diseño de SQX_Library, no una fórmula de autor externo),
 * propuesto el 2026-07-11 (segunda ronda). Ver justificación completa en:
 * user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Gap Index. Mide el gap de apertura (Apertura de hoy - Cierre de ayer) normalizado
 * por ATR — responde "¿qué tan grande fue el gap en términos de volatilidad típica
 * reciente del activo?", no en pips/puntos crudos. Diseño propio (no es una fórmula
 * publicada con autor específico), pensado para plantillas de familia "Gap de
 * apertura" en índices/CFD, donde un gap crudo en puntos no es comparable entre
 * activos con distinta escala de precio.
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator SMA
 * para el ATR, calculado a mano como en ChandelierExit.java).
 */
package SQ.Blocks.Indicators.GapIndex;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(GAPIDX) Gap Index", display="GapIndex(@Chart@#AtrPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Gap Index (diseno propio SQX_Library). Gap de apertura normalizado por ATR, para comparar el tamano del gap entre activos de distinta escala de precio. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="AtrPeriod=14")
public class GapIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="AtrPeriod", minValue=2, maxValue=200, defaultValue="14", step=1)
	public int AtrPeriod;

	@Output(name = "GapIndex", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator atrCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		atrCalc = new AverageCalculator(AverageCalculator.SMA, AtrPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double h = Chart.High.get(0), l = Chart.Low.get(0);
		double open = Chart.Open.get(0);
		double prevClose = (getCurrentBar() > 0) ? Chart.Close.get(1) : open;

		double tr;
		if (getCurrentBar() == 0) {
			tr = h - l;
		} else {
			tr = Math.max(h - l, Math.max(Math.abs(h - prevClose), Math.abs(l - prevClose)));
		}
		atrCalc.onBarUpdate(tr, getCurrentBar());
		double atr = atrCalc.getValue();

		double gap = open - prevClose;
		double gapIndex = (atr < 0.0000000001) ? 0 : gap / atr;

		Value.set(0, gapIndex);
	}
}
