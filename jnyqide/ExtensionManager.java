package jnyqide;


import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.AbstractListModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;


public class ExtensionManager extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTable table;
	private JPanel buttonPane = new JPanel();

	/**
	 * Create the dialog for Extension Manager
	 */
	public ExtensionManager() 
	{
		// Set the extension manager window properties
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle("Extension Manager");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Add the table for the list of extensions
		AddTableToWindow();
		
		// Add the lower panel for buttons
		buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		// Add the 'Install' button
		AddInstallButton();
		
		// Add the 'Cancel' button
		AddCancelButton();
	}
	
	// ==================== Window-creation related functions ========================================

	/**
	 * This function adds a table grid for the list of extensions on the window 
	 */
	private void AddTableToWindow()
	{
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			table = new JTable();
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setCellSelectionEnabled(false);
			table.setColumnSelectionAllowed(false);
			table.setRowSelectionAllowed(true);
			String header[] = new String[] { "Package", "Ver.", "Date", "Description", "URL", "Checksum" };
			DefaultTableModel dtm = new DefaultTableModel(0, 0)
			{
				Class[] columnTypes = new Class[] {
						String.class, String.class, String.class, String.class, String.class, String.class
				};
				public Class getColumnClass(int columnIndex) {
					return columnTypes[columnIndex];
				}
				boolean[] columnEditables = new boolean[] {
					false, false, false, false, false, false
				};
				public boolean isCellEditable(int row, int column) {
					return columnEditables[column];
				}
			};
			dtm.setColumnIdentifiers(header);
			table.setModel(dtm);
			{
				table.getColumnModel().getColumn(0).setPreferredWidth(182);
				table.getColumnModel().getColumn(1).setPreferredWidth(100);
				table.getColumnModel().getColumn(2).setPreferredWidth(150);
				table.getColumnModel().getColumn(3).setPreferredWidth(302);
				table.getColumnModel().getColumn(4).setPreferredWidth(212);
				table.getColumnModel().getColumn(5).setPreferredWidth(150);
				getContentPane().add(table, BorderLayout.NORTH);
			}
			getContentPane().add(new JScrollPane(table));

			
			String[] extensions = LoadExtensionData(ExtensionListLink);
			
			for (int i = 0; i < extensions.length; ++i)
			{
				String[] ext = SplitExtensionData(extensions[i], 5);
		        dtm.addRow(new Object[] { ext[0], ext[2], ext[3], ext[4], ext[1] });
			}
		}
	}
	
	/**
	 * This function adds 'Install' button on the window.
	 * Pressing this button will install the packages selected in the table. 
	 */
	private void AddInstallButton()
	{
		JButton installButton = new JButton("Install");

		// Define 'Install' button functionality 
		installButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				String extDir = System.getenv("NYQEXTPATH");
				
				// To me: add boolean condition below
				MakeDirectory(extDir);
				
				int[] selectedRows = table.getSelectedRows();
				for (int i = 0; i < selectedRows.length; ++i)
				{
					String packageName = table.getModel().getValueAt(selectedRows[i], 0).toString();
					String packageDir = extDir + File.separator + packageName;
					MakeDirectory(packageDir);
					String link = table.getModel().getValueAt(selectedRows[i], 4).toString();
					String fileContent = SaveFromURL(link, packageDir);
					String[] otherFiles = ExtractOtherFilesFromSAL(fileContent);
					for (int j = 0; j < otherFiles.length; ++j)
					{
						String fileLink = DirFromGithubURL(link) + otherFiles[j];
						fileContent = SaveFromURL(fileLink, packageDir);
					}
				}
				JOptionPane.showMessageDialog(contentPanel, "Completed installing the selected packages!");
			}
		});

		installButton.setActionCommand("Install");
		buttonPane.add(installButton);
		getRootPane().setDefaultButton(installButton);
	}
	
	/**
	 * This function adds 'Cancel' button on the window.
	 * Pressing this button will close the Extension Manager window.
	 */
	private void AddCancelButton()
	{
		JButton cancelButton = new JButton("Cancel");

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);
	}
	
	// ====================== Project-dependent functions ============================================

	/**
	 * URL of the Extension list. Should directly point to the text file.
	 */
	private static String ExtensionListLink = "https://raw.githubusercontent.com/keipour/nyquist-extensions/master/extlist.txt";

	
	/**
	 * This function retrieves and returns the list of additional files mentioned in 
	 * a Nyquist extension file. The function assumes the following conditions:
	 * 
	 * 1- The name of each additional file is in a separate line with the following format:
	 *    ;;  Additional-File:  <additional-file-name>
	 * 
	 * 2- The function only reads the extension information section of the input file, 
	 * and stops parsing on the first line that is not formatted as such. In other words, 
	 * it will only parse the upper section of the file where all the lines are either empty
	 * or start with ;;
	 * 
	 * Returns null on any type of error or if no additional files found
	 */
	private static String[] ExtractOtherFilesFromSAL(String fileContent)
	{
		try
		{
			// Extract lines from the SAL file
			String[] lines = fileContent.split(System.getProperty("line.separator"));
			
			// Parse the header of SAL file to find the additional files 
			List<String> fileNames = new ArrayList<String>();
			for (int i = 0; i < lines.length; ++i)
			{
				String line = lines[i].trim();
				
				// Ignore the line if it is empty
				if (line.isEmpty()) continue;
				
				// Stop parsing if we reach the end of extension info section 
				// (i.e. stop if there is a line not starting with ;;)
				if (line.substring(0, 2).equals(";;") == false) break;
				
				try
				{
					line = line.substring(2).trim();
					
					// If the line contains an additional file, extract it and add it to the list
					String searchTerm = "Additional-File:";
					if (line.substring(0, searchTerm.length()).equalsIgnoreCase(searchTerm) == true)
						fileNames.add(line.substring(searchTerm.length()).trim());
				}
				catch (Exception e)
				{
					continue;
				}
			}
			
			// Convert the file list to an array
			String[] result = new String[fileNames.size()];
			result = fileNames.toArray(result);
	
			return result;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * This function obtains extension information from a string line read from the 
	 * extension list file. It assumes the following format:
	 * info[1], info[2], ... , info[numOfCells]
	 * Last cell can contain ',' or any other character, while the first numOfCells-1 
	 * cells should not contain any commas.
	 * Returns null on any type of error
  	 */
	private static String[] SplitExtensionData(String line, int numOfCells)
	{
		try
		{
			String[] result = new String[numOfCells];
			result = line.split(",", numOfCells);
			for (int i = 0; i < result.length; ++i)
				result[i] = result[i].trim();
			return result;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * This function reads the extension list from the given URL and returns an 
	 * array of extension data (information for each extension in one cell.
	 * Assumes that the extension list has info for each extension on a separate line
	 * Returns null on any type of error
	 */
	private static String[] LoadExtensionData(String link)
	{
		try
		{
			return ReadFromURL(link).split(System.getProperty("line.separator"));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	
	
	// ===================== Project-independent functions ===========================================
	
	/**
	 * Reads a text file from the given URL and returns it as a String object
	 * Returns null on any type of error
	 */
	private static String ReadFromURL(String link)
	{
		try
		{
		    URL url = new URL(link);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			Map<String, List<String>> header = http.getHeaderFields();

			InputStream stream = http.getInputStream();
			return GetStringFromStream(stream);
		} 
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	/**
	 * Reads a text file from a stream and returns it as a String object
	 * Returns null on any type of error
	 */
	private static String GetStringFromStream(InputStream stream) 
	{
		if (stream != null) 
		{
			Writer writer = new StringWriter();
 
			char[] buffer = new char[2048];
			try 
			{
				Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
				int counter;
				while ((counter = reader.read(buffer)) != -1) 
					writer.write(buffer, 0, counter);

				stream.close();
				return writer.toString();
			}
			catch (Exception e)
			{
				return null;
			}
		} 
		return null;
	}
	
	
	/**
	 * Reads a text file from a stream and saves it in a local directory
	 * Additionally, returns the read stream as a String object (only if saved successfully)
	 * Returns null on any type of error
	 */
 	private static String SaveFromURL(String link, String dir)
	{
		String fileContent =  ReadFromURL(link);
		if (fileContent == null) return null;
		
		String filename = ExtractFilenameFromGithubURL(link); 
		String filepath = dir + File.separator + filename;
		
		try
		{
			PrintWriter out = new PrintWriter(filepath);
			out.println(fileContent);
			out.close();
			return fileContent;
		}
		catch (Exception e)
		{ 
			return null; 
		}
	}
	
 	
	/**
 	 * Extract a file name from an URL. Only works if the filename is the last 
 	 * part of the URL after the last slash ('/')
 	 * E.g. this function returns 'extlist.txt' from the URL below 
 	 * https://raw.githubusercontent.com/keipour/nyquist-extensions/master/extlist.txt
	 */
	private static String ExtractFilenameFromGithubURL(String link)
	{
		try 
		{
			return link.substring(link.lastIndexOf('/') + 1);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	
	/**
	 * Extract the URL for directory from an URL, i.e. removes the file name from the 
	 * input URL and returns the rest, including the '/' at the end. Only works if the directory is the 
	 * part of the URL right before the last slash ('/'). 
	 * E.g. this function removes 'extlist.txt' from the URL below 
	 * https://raw.githubusercontent.com/keipour/nyquist-extensions/master/extlist.txt
	 */
	private static String DirFromGithubURL(String link)
	{
		try 
		{
			return link.substring(0, link.lastIndexOf('/') + 1);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	
	/**
	 * Create a local directory at the given path
	 * Returns false on any kind of error
 	 */
	private static boolean MakeDirectory(String path)
	{
		File theDir = new File(path);
		
		// If the directory does not exist, create it
		if (!theDir.exists()) 
		{
		    try
		    {
		        theDir.mkdir();
		        return true;
		    } 
		    catch (Exception e) 
		    {
		    	return false;
		    }        
		}
		return true;
	}
}
