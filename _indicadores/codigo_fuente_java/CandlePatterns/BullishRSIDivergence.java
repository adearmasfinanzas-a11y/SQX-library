/*
 * Patrón no nativo (comparador multi-vela, no un patrón de vela clásico), propuesto
 * para SQX_Library el 2026-07-11 (segunda ronda). Ver justificación completa en:
 * user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Divergencia alcista RSI/Precio. Busca los dos "swing lows" (mínimos locales) más
 * recientes del precio dentro de una ventana de lookback; si el precio hizo un
 * mínimo más BAJO en el swing más reciente, pero el RSI calculado en ese mismo punto
 * es más ALTO que en el swing anterior, hay divergencia alcista — señal clásica de
 * agotamiento del movimiento bajista. Los comparadores nativos son de umbral fijo
 * (RSI > X), no comparan patrones entre dos puntos de giro — este es un mecanismo
 * genuinamente distinto.
 *
 * NOTA DE DISEÑO: el RSI se calcula de forma simplificada (promedio simple de
 * ganancias/pérdidas, no el suavizado recursivo de Wilder) porque se recalcula de
 * forma independiente y sin estado en cada punto de giro evaluado — coherente con
 * el patrón real de los ConditionBlock nativos (ej. Hammer.java), que son stateless
 * y recalculan todo desde Chart.X(shift) en cada evaluación.
 *
 * Escrito siguiendo el patrón real de los patrones de vela nativos (ConditionBlock,
 * acceso Chart.High(shift)/Low(shift)/Close(shift) por método).
 */
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Bullish RSI Divergence pattern", display="Bullish RSI Divergence(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Divergencia alcista: el precio hace un minimo mas bajo pero el RSI hace un minimo mas alto en el swing correspondiente. Señal de agotamiento bajista. Patrón no nativo agregado a SQX_Library.")
@OppositeBlock("BearishRSIDivergence")
public class BullishRSIDivergence extends ConditionBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="1")
	public int Shift;

	@Parameter(defaultValue="30", minValue=10, maxValue=100, step=1)
	public int LookbackPeriod;

	@Parameter(defaultValue="2", minValue=1, maxValue=5, step=1)
	public int SwingStrength;

	@Parameter(defaultValue="14", minValue=2, maxValue=50, step=1)
	public int RSIPeriod;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private double simpleRSI(int shiftBack) {
		double sumGain = 0, sumLoss = 0;
		for (int i = 0; i < RSIPeriod; i++) {
			double curr = Chart.Close(shiftBack + i);
			double prev = Chart.Close(shiftBack + i + 1);
			double change = curr - prev;
			if (change > 0) sumGain += change; else sumLoss += -change;
		}
		double avgGain = sumGain / RSIPeriod;
		double avgLoss = sumLoss / RSIPeriod;
		if (avgLoss < 0.0000000001) return 100;
		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	private boolean isSwingLow(int barShift) {
		double centerLow = Chart.Low(barShift);
		for (int s = 1; s <= SwingStrength; s++) {
			if (Chart.Low(barShift - s) < centerLow) return false;
			if (Chart.Low(barShift + s) < centerLow) return false;
		}
		return true;
	}

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		int firstSwingShift = -1, secondSwingShift = -1;

		for (int i = Shift + SwingStrength; i <= Shift + LookbackPeriod - SwingStrength; i++) {
			if (isSwingLow(i)) {
				if (firstSwingShift == -1) {
					firstSwingShift = i;
				} else {
					secondSwingShift = i;
					break;
				}
			}
		}

		if (firstSwingShift == -1 || secondSwingShift == -1) return false;

		double recentLow = Chart.Low(firstSwingShift);
		double olderLow = Chart.Low(secondSwingShift);

		double recentRSI = simpleRSI(firstSwingShift);
		double olderRSI = simpleRSI(secondSwingShift);

		return (recentLow < olderLow) && (recentRSI > olderRSI);
	}
}
