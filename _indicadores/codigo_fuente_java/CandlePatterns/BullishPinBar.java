/*
 * Patrón de vela no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Pin Bar alcista (price-action moderno, no es un patrón japonés clásico de TA-Lib):
 * mecha inferior larga (>= 2/3 del rango total de la vela) + cuerpo pequeño en el
 * extremo superior — rechazo agresivo de un nivel de precio a la baja.
 * Fuente: https://dailypriceaction.com/blog/forex-pin-bar-trading-strategy/
 *
 * Escrito siguiendo el patrón real de los patrones de vela nativos
 * (SQ/Blocks/CandlePatterns/Hammer.java — extends ConditionBlock, acceso
 * Chart.High(shift)/Low(shift)/Open(shift)/Close(shift) por método, no por .get()).
 */
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Bullish Pin Bar pattern", display="Bullish Pin Bar(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Pin Bar alcista: mecha inferior larga (rechazo) con cuerpo pequeño en el extremo superior. Patrón no nativo agregado a SQX_Library.")
@OppositeBlock("BearishPinBar")
public class BullishPinBar extends ConditionBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="1")
	public int Shift;

	@Parameter(defaultValue="0.66", minValue=0.4, maxValue=0.9, step=0.01)
	public double MinWickRatio;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		double H = Chart.High(Shift);
		double L = Chart.Low(Shift);
		double O = Chart.Open(Shift);
		double C = Chart.Close(Shift);

		double totalRange = H - L;
		if (totalRange < 0.0000000001) return false;

		double bodyHigh = Math.max(O, C);
		double bodyLow = Math.min(O, C);
		double lowerWick = bodyLow - L;
		double upperWick = H - bodyHigh;

		boolean longLowerWick = (lowerWick / totalRange) >= MinWickRatio;
		boolean smallOppositeWick = upperWick <= (totalRange * (1 - MinWickRatio) * 0.5);

		return longLowerWick && smallOppositeWick;
	}
}
