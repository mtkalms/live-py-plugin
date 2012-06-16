package live_py;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.framework.Bundle;


public class LiveCodingResultsColumn extends LineNumberRulerColumn {
	private ITextViewer cachedTextViewer;
	private ArrayList<String> results;
	private final int MAX_WIDTH = 60;
	private int width;
	private String[] scriptArguments;
	private String[] environment;

	@Override
	public Control createControl(CompositeRuler parentRuler,
			Composite parentControl) {
		cachedTextViewer= parentRuler.getTextViewer();
		results = new ArrayList<String>();
		return super.createControl(parentRuler, parentControl);
	}
	
	@Override
	protected String createDisplayString(int line) {
		return line < results.size() ? results.get(line) : "";
	}
	
	@Override
	protected int computeNumberOfDigits() {
		
		checkEnvironment();
		checkArguments();
		String text = cachedTextViewer.getDocument().get();
		Runtime runtime = Runtime.getRuntime();
		width = 5;
		try {
			Process process = runtime.exec(scriptArguments, environment);
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(process.getOutputStream()));
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			
			try {
				writer.write(text);
				writer.close();
				
				results.clear();
				String line;
				do {
					line = reader.readLine();
					if (line != null) {
						results.add(line);
						width = Math.max(width, line.length());
					}
				} while (line != null);
			} finally {
				writer.close();
				reader.close();
			}
		} catch (Exception e) {
			results.clear();
			results.add(e.getMessage());
		}
		width = Math.min(width, MAX_WIDTH);
		for (int j = 0; j < results.size(); j++) {
			String line = results.get(j);
			if (line.length() > width) {
				line = line.substring(0, width);
			}
			results.set(j, String.format("%1$-" + width + "s", line));
		}
		
		return width;
	}

	private void checkEnvironment() {
		if (environment != null)
		{
			return;
		}
			
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = 
				workbench == null ? null : workbench.getActiveWorkbenchWindow();
		IWorkbenchPage activePage = 
				window == null ? null : window.getActivePage();
		
		IEditorPart editor = 
				activePage == null ? null : activePage.getActiveEditor();
		IEditorInput input = 
				editor == null ? null : editor.getEditorInput();
		IPath path = input instanceof FileEditorInput 
				? ((FileEditorInput)input).getPath()
				: null;
		if (path != null)
		{
			String pythonPath = String.format(
					"PYTHONPATH=%1$s", 
					path.removeLastSegments(1));
			environment = new String[] {pythonPath};
		}
	}
	
	private void checkArguments() {
		if (scriptArguments != null)
		{
			return;
		}
		try {
			Bundle bundle = Activator.getDefault().getBundle();
			Path path = new Path("PySrc/code_tracer.py");
			URL bundleURL = FileLocator.find(bundle, path, null);
			URL fileURL;
			fileURL = FileLocator.toFileURL(bundleURL);
			Path path2 = new Path("PySrc/report_builder.py");
			URL bundle2URL = FileLocator.find(bundle, path2, null);
			FileLocator.toFileURL(bundle2URL);
			scriptArguments = new String[] {"python", fileURL.getPath()};
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
