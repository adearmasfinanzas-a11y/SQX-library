/*
 * Patrón de vela no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Inside Bar + ruptura alcista (price-action moderno — emparentado con Harami/Hikkake
 * nativos pero usado aquí específicamente como filtro de contracción de volatilidad
 * + ruptura, no como patrón de reversión en sí). Convención de índices: #Shift# es la
 * vela de ruptura evaluada, #Shift#+1 es la vela interior (inside bar), #Shift#+2 es
 * la vela "madre" que la contiene.
 * Fuente: https://www.mql5.com/en/articles/19738
 *
 * Escrito siguiendo el patrón real de los patrones de vela nativos (Hammer.java).
 */
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Inside Bar Breakout Up pattern", display="Inside Bar Breakout Up(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Vela interior (rango contenido en la vela anterior) seguida de ruptura alcista de ese rango contenido. Patrón no nativo agregado a SQX_Library.")
@OppositeBlock("InsideBarBreakoutDown")
public class InsideBarBreakoutUp extends ConditionBlock {

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

		return breakoutClose > insideHigh;
	}
}
