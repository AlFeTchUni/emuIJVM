package GUI;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class Stack
{
	//il disegno del registro
	private int[][] xArr;
	private int[][] yArr;
	private int[] values;
	//determina se lo stack è un underflow
	private boolean error;
	//il disegno dell'errore
	private int[] xErr;
	private int[] yErr;
	//dove punta lv
	private int lv;
	//il link pointer
	private int linkPtr;
	//la freccia verso sinistra
	private int[] dxArrowX;
	private int[] dxArrowY;
	//la freaccia verso destra
	private int[] sxArrowX;
	private int[] sxArrowY;

	public Stack()
	{
		xArr = new int[0][4];
		yArr = new int[0][4];
		values = new int[0];
		xErr = new int[4];
		yErr = new int[4];
		//il disegno dell'errore
		xErr[0] = 80;
		xErr[1] = 200;
		xErr[2] = 200;
		xErr[3] = 80;
		yErr[0] = 645;
		yErr[1] = 645;
		yErr[2] = 625;
		yErr[3] = 625;
		//il disegno della freccia verso sinistra
		dxArrowX = new int[3];
		dxArrowX[0] = 215;
		dxArrowX[1] = 215;
		dxArrowX[2] = 205;
		dxArrowY = new int[3];
		dxArrowY[0] = 8;
		dxArrowY[1] = 16;
		dxArrowY[2] = 12;
		//il disegno della freccia verso destra
		sxArrowX = new int[3];
		sxArrowX[0] = 75;
		sxArrowX[1] = 75;
		sxArrowX[2] = 80;
		sxArrowY = new int[3];
		sxArrowY[0] = 8;
		sxArrowY[1] = 16;
		sxArrowY[2] = 12;
	}

	public void insertRegister()
	{
		int[][] xArrN = new int[xArr.length + 1][4];
		int[][] yArrN = new int[xArr.length + 1][4];
		int[] valuesN = new int[xArr.length + 1];
		for (int i = 0; i < xArr.length; i++)
		{
			xArrN[i] = xArr[i];
			yArrN[i] = yArr[i];
			valuesN[i] = values[i];
		}
		xArr = xArrN;
		yArr = yArrN;
		values = valuesN;
		xArr[xArr.length - 1][0] = 80;
		xArr[xArr.length - 1][1] = 200;
		xArr[xArr.length - 1][2] = 200;
		xArr[xArr.length - 1][3] = 80;
		yArr[xArr.length - 1][0] = 645 - (xArr.length - 1 >= 29 ? 29 : xArr.length - 1) * 21;
		yArr[xArr.length - 1][1] = 645 - (xArr.length - 1 >= 29 ? 29 : xArr.length - 1) * 21;
		yArr[xArr.length - 1][2] = 645 - ((xArr.length - 1 >= 29 ? 29 : xArr.length - 1) * 21) - 20;
		yArr[xArr.length - 1][3] = 645 - ((xArr.length - 1 >= 29 ? 29 : xArr.length - 1) * 21) - 20;
		if (xArr.length > 29)
			up();
		error = false;
	}

	//sposta tutti i registri verso il basso
	public void up()
	{
		for (int i = 0; i < yArr.length; i++)
			for (int j = 0; j < 4; j++)
				yArr[i][j] += 21;
	}

	//sposta tutti i registri verso l'alto
	public void down()
	{
		for (int i = 0; i < yArr.length; i++)
			for (int j = 0; j < 4; j++)
				yArr[i][j] -= 21;
	}

	public void setStackValues(int[] toSet)
	{
		//aggiorno i dati della cima dello stack ed lv
		for (int i = lv = values.length - toSet.length, j = 0; i < values.length && j < toSet.length; j++, i++)
			values[i] = toSet[j];
		//calcolo la posizione di linkPtr
		if (values.length > lv)
			linkPtr = values[lv] - ((int) Math.pow(2, 25)) + 1;
		else if (values.length != 1)
			error();
	}


	public void deleteRegister()
	{
		if (xArr.length != 0)
		{
			int[][] xArrN = new int[xArr.length - 1][4];
			int[][] yArrN = new int[xArr.length - 1][4];
			int[] valuesN = new int[values.length - 1];
			for (int i = 0; i < xArrN.length; i++)
			{
				xArrN[i] = xArr[i];
				yArrN[i] = yArr[i];
				valuesN[i] = values[i];
			}
			xArr = xArrN;
			yArr = yArrN;
			values = valuesN;
			if (xArr.length >= 29)
				down();
		} else
			error = true;
	}

	public void error()
	{
		error = true;
	}

	public void paintComponent(GraphicsContext g)
	{
		if (!error)
		{
			for (int i = 0; i < xArr.length; i++)
			{
				g.setFill(Color.GRAY);
				double[] xArrDoubles = Arrays.stream(xArr[i]).asDoubleStream().toArray();
				double[] yArrDoubles = Arrays.stream(yArr[i]).asDoubleStream().toArray();
				g.fillPolygon(xArrDoubles, yArrDoubles, 4);
				g.setFill(Color.WHITE);
				String toPrint = new Integer(values[i]).toString();
				g.fillText(toPrint, 140 - ((toPrint.length() * 7) / 2), yArr[i][0] - 5);
				//stampo la freccia verso destra che indica LV

			}
			if (xArr.length > 1)
			{
				g.setFill(Color.BLACK);
				int[] nArrowY = new int[6];
				//stampo le freccie di LV ed SP
				if (lv < xArr.length)
				{
					for (int i = 0; i < 3; i++)
						nArrowY[i] = dxArrowY[i] + yArr[lv][3];

					double[] dxArrowXDoubles = Arrays.stream(dxArrowX).asDoubleStream().toArray();
					double[] nArrowYDoubles = Arrays.stream(nArrowY).asDoubleStream().toArray();
					g.fillPolygon(dxArrowXDoubles, nArrowYDoubles, 3);
					g.fillText("LV", 218, nArrowY[1]);
				}
				for (int i = 0; i < 3; i++)
					nArrowY[i] = dxArrowY[i] + yArr[yArr.length - 1][3];

				double[] dxArrowXDoubles = Arrays.stream(dxArrowX).asDoubleStream().toArray();
				double[] nArrowYDoubles = Arrays.stream(nArrowY).asDoubleStream().toArray();
				g.fillPolygon(dxArrowXDoubles, nArrowYDoubles, 3);
				g.fillText("SP", 218, nArrowY[1]);
				//stampo la freccia di linkPtr
				int[] x = {80, 60, 60, 75};
				if (linkPtr < yArr.length)
				{
					int[] y = {yArr[lv][3] + 10, yArr[lv][3] + 10, yArr[linkPtr][3] + 10, yArr[linkPtr][3] + 10};
					double[] xDoubles = Arrays.stream(x).asDoubleStream().toArray();
					double[] yDoubles = Arrays.stream(y).asDoubleStream().toArray();
					g.strokePolyline(xDoubles, yDoubles, 4);
					int[] nSxArrowY = new int[3];
					nSxArrowY[0] = sxArrowY[0] + yArr[linkPtr][3] - 2;
					nSxArrowY[1] = sxArrowY[1] + yArr[linkPtr][3] - 2;
					nSxArrowY[2] = sxArrowY[2] + yArr[linkPtr][3] - 2;

					double[] sxArrowXDoubles = Arrays.stream(sxArrowX).asDoubleStream().toArray();
					double[] nSxArrowYDoubles = Arrays.stream(nSxArrowY).asDoubleStream().toArray();
					g.fillPolygon(sxArrowXDoubles, nSxArrowYDoubles, 3);
				}
			}
		} else
		{
			g.setFill(Color.RED);
			double[] xErrDoubles = Arrays.stream(xErr).asDoubleStream().toArray();
			double[] yErrDoubles = Arrays.stream(yErr).asDoubleStream().toArray();
			g.fillPolygon(xErrDoubles, yErrDoubles, 4);
			g.setFill(Color.WHITE);
			g.fillText("Stack Error", 110, yErr[0] - 5);
			error = false;
		}
	}

}
