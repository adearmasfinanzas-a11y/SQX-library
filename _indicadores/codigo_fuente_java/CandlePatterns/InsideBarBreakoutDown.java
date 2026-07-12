/*
 * Patrón de vela no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Inside Bar + ruptura bajista (espejo de InsideBarBreakoutUp.java).
 * Fuente: https://www.mql5.com/en/articles/19738
 */
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Inside Bar Breakout Down pattern", display="Inside Bar Breakout Down(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Vela interior (rango contenido en la vela anterior) seguida de ruptura bajista de ese rango contenido. Patrón no nativo agregado a SQX_Library.")
@OppositeBlock("InsideBarBreakoutUp")
public class InsideBarBreakoutDown extends ConditionBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="1")
	public int Shift;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		double motherHigh = Chart.High(Shift + 2);
		double motherLow = Chart.Low(Shift + 2);

		double insideHigh = Chart.High(Shift + 1);
		double insideLow = Chart.Low(Shift + 1);

		boolean isInside = (insideHigh <= motherHigh) && (insideLow >= motherLow);
		if (!isInside) return false;

		double breakoutClose = Chart.Close(Shift);

		return breakoutClose < insideLow;
	}
}
