package jnyqide;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.print.attribute.standard.OutputDeviceAssigned;
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



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileInputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;

import java.security.MessageDigest;

public class ExtensionManager extends JDialog {

	/**
	 * URL of the Extension list. Should directly point to the text file.
	 */
	private static String ExtensionListLink = "https://raw.githubusercontent.com/keipour/nyquist-extensions/master/extlist.txt";

	
	/**
	 * Name of the environment variable containing the path for extensions directory.
	 */
	private static String ExtensionDirectoryEnvVariable = "NYQEXTPATH";
	

	
	private final JPanel contentPanel = new JPanel();
	private JTable table;
	private JPanel buttonPane = new JPanel();
	private DefaultTableModel dtm;

	
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

		// Add the buttons
		AddInstallButton();
		AddUpdateButton();
		AddCancelButton();
	}
	
	// ==================== Window-creation related functions ========================================

	/**
	 * This function adds a table grid for the list of extensions on the window.
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
			dtm = new DefaultTableModel(0, 0)
			{
				Class[] columnTypes = new Class[] {
						String.class, String.class, String.class, String.class, String.class, String.class
				};
				public Class getColumnClass(int columnIndex) {
					return columnTypes[columnIndex];
				}
				boolean[] columnEditables = new boolean[] {
					false, false, false, false, false, true
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

			LoadExtensionDataToTable(true);
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
				try
				{
					// Create the extensions directory at the path defined in the environment variable
					String extDir = CreateDirectoryFromEnvVariable(ExtensionDirectoryEnvVariable); 
					if (extDir == null) return;
					
					// Define lists for installed and failed packages (used for the final message)
					List<String> installedPackages = new ArrayList<String>();
					List<String> failedPackages = new ArrayList<String>();
					
					// Retrieve the selected rows from table
					int[] selectedRows = table.getSelectedRows();
					if (selectedRows.length == 0)
					{
						JOptionPane.showMessageDialog(contentPanel, "No packages selected to install!", 
								"Packages", JOptionPane.WARNING_MESSAGE);
						return;
					}
					
					// Install the selected packages
					for (int i = 0; i < selectedRows.length; ++i)
					{
						// Create a sub-directory in extensions directory with package name
						String packageName = table.getModel().getValueAt(selectedRows[i], 0).toString();
						String packageDir = extDir + File.separator + packageName;
						if (MakeDirectory(packageDir) == false)
						{
							JOptionPane.showMessageDialog(contentPanel, "Error creating directory at the following path:\n'" + 
									packageDir + "'\n\nPackage '" + packageName + "' not installed!", 
									"Directory Creation Problem", JOptionPane.ERROR_MESSAGE);
							
							failedPackages.add(packageName);
							continue;
						}
	
						// Read the contents of the selected package file
						String link = table.getModel().getValueAt(selectedRows[i], 4).toString();
						String fileContent = SaveFromURL(link, packageDir);
						if (fileContent == null)
						{
							JOptionPane.showMessageDialog(contentPanel, "Error reading the following URL:\n'" + 
									link + "'\n\nPackage '" + packageName + "' not installed!", 
									"Error", JOptionPane.ERROR_MESSAGE);
							
							failedPackages.add(packageName);
							continue;
						}
						
						// Keep the list of all the files for checksum
						List<String> packageFiles = new ArrayList<String>();
						packageFiles.add(FileLocalPathFromURL(link, packageDir));
						
						// Parse the extension file to read the additional files  
						String[] otherFiles = ExtractOtherFilesFromSAL(fileContent);
						if (otherFiles == null)
						{
							JOptionPane.showMessageDialog(contentPanel, "Unknown error happened while parsing the extension file!\n'" + 
								link + "'\n\nPackage '" + packageName + "' not installed!\n", 
								"Error", JOptionPane.ERROR_MESSAGE);
							failedPackages.add(packageName);
							continue;
						}
						
						// Download the additional files
						for (int j = 0; j < otherFiles.length; ++j)
						{
							String fileLink = DirFromGithubURL(link) + otherFiles[j];
							fileContent = SaveFromURL(fileLink, packageDir);
							packageFiles.add(FileLocalPathFromURL(fileLink, packageDir));
						}
						
						// Compare the checksum of the downloaded files with the reference checksum 
						String referenceChecksum = table.getModel().getValueAt(selectedRows[i], 5).toString(); 
						String downloadedChecksum = CalculateFileChecksum(packageFiles);
						if (downloadedChecksum.equalsIgnoreCase(referenceChecksum))
							installedPackages.add(packageName);
						else // remove the downloaded package if checksum is different
						{
							// Remove the downloaded files from the system
							RemoveFiles(packageFiles);
							
							JOptionPane.showMessageDialog(contentPanel, "Checksum of the available package is different than the verified version!\n" + 
									"Package '" + packageName + "' not installed!\n", 
									"Checksum Error", JOptionPane.ERROR_MESSAGE);
							failedPackages.add(packageName);
						}
					}
					
					// Show a message about the completion of installation
					ShowPostInstallationMessage(installedPackages, failedPackages);
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(contentPanel, "An unknown error happened installing the packages!\nNo packages installed!\n" + 
							"Error message: " + e.toString(), 
							"Unknown Problem", JOptionPane.ERROR_MESSAGE);
				}
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
	
	
	/**
	 * This function adds 'Update' button on the window.
	 * Pressing this button will update the extensions list from the remote server.
	 */
	private void AddUpdateButton()
	{
		JButton updateButton = new JButton("Update");

		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				LoadExtensionDataToTable(true);
			}
		});
		
		updateButton.setActionCommand("Update");
		buttonPane.add(updateButton);
	}

	// ====================== Project-dependent functions ============================================

	/**
	 * Loads the extension data from to the grid table.
	 */
	private void LoadExtensionDataToTable(boolean fromNetwork)
	{
		try
		{
			dtm.setRowCount(0);
			
			String[] extensions = LoadExtensionData(ExtensionListLink);
			
			for (int i = 0; i < extensions.length; ++i)
			{
				// Ignore the line if it is a comment or if it is empty
				if (extensions[i].trim().isEmpty()) continue;
				if (extensions[i].trim().startsWith("#")) continue;
									
				String[] ext = SplitExtensionData(extensions[i], 6);
		        dtm.addRow(new Object[] { ext[0], ext[2], ext[3], ext[5], ext[1], ext[4] });
			}
		}
		catch (Exception e)
		{	
			JOptionPane.showMessageDialog(contentPanel, "Error loading the extension list from the following path: \n'" + 
					ExtensionListLink + "'" + "\n\n" + e.toString(), "Error Loading Extensions", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	/**
	 * Shows a message after installation of the packages.
	 */
	private void ShowPostInstallationMessage(List<String> installedPackages, List<String> failedPackages)
	{
		// Announce the failed and installed packages
		String endMessage = "Completed installation!";
		int messageType = -1;
		if (installedPackages.size() > 0)
		{
			endMessage += "\n\nThe following package(s) were successfully installed:\n";
			for (String pkg : installedPackages) 
				endMessage += "'" + pkg + "'  ";
			messageType = JOptionPane.INFORMATION_MESSAGE;
		}
		if (failedPackages.size() > 0)
		{
			endMessage += "\n\nInstallation of the following package(s) failed:\n";
			for (String pkg : failedPackages) 
				endMessage += "'" + pkg + "'  ";
			if (messageType == -1)
				messageType = JOptionPane.ERROR_MESSAGE;
			else
				messageType = JOptionPane.WARNING_MESSAGE;
		}
		
		if (messageType == JOptionPane.ERROR_MESSAGE)
			JOptionPane.showMessageDialog(contentPanel, "No packages were installed!", "Job Completed", messageType);
		else
			JOptionPane.showMessageDialog(contentPanel, endMessage, "Job Completed", messageType);
	}
	
	
	/**
	 * Creates a directory at the path defined in a given environment variable. 
	 * Returns the directory path read from the environment variable. 
	 * 
	 * Returns null on any type of error.
	 */
	private String CreateDirectoryFromEnvVariable(String envVar)
	{
		// Read the path for directory  
		String dirPath = System.getenv(envVar);
		if (dirPath == null)
		{
			JOptionPane.showMessageDialog(contentPanel, "Error! Extension directory path is not defined in '" + 
					envVar + "' environment variable!\nNo packages installed!", 
					"Environment Variable Problem", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		// Create the extensions directory
		if (MakeDirectory(dirPath) == false)
		{
			JOptionPane.showMessageDialog(contentPanel, "Error creating directory at the following path:\n'" + 
					dirPath + "'\n\nNo packages installed!", 
					"Directory Creation Problem", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		// Return the path if everything was ok
		return dirPath;
	}
	
	
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
	 * or start with double semicolons (;;).
	 * 
	 * Returns null on any type of error or if no additional files found.
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
	 * Returns null on any type of error.
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
	 * Assumes that the extension list has info for each extension on a separate line.
	 * Returns null on any type of error.
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
	 * Reads a text file from the given URL and returns it as a String object.
	 * Returns null on any type of error.
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
	 * Reads a text file from a stream and returns it as a String object.
	 * Returns null on any type of error.
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
	 * Reads a text file from a stream and saves it in a local directory.
	 * Additionally, returns the read stream as a String object (only if saved successfully).
	 * Returns null on any type of error.
	 */
 	private static String SaveFromURL(String link, String dir)
	{
		String fileContent =  ReadFromURL(link);
		if (fileContent == null) return null;
		
		String filepath = FileLocalPathFromURL(link, dir);
		if (filepath == null) return null;

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
 	 * Generates the local path for a file from its URL and its local directory.
	 * Returns null on any type of error.
 	 */
 	private static String FileLocalPathFromURL(String link, String dir)
 	{
		try
		{
			String filename = ExtractFilenameFromGithubURL(link); 
			return dir + File.separator + filename;
		}
		catch(Exception e)
		{
			return null;
		}
 	}
 	
	
 	/**
 	 * Extract a file name from an URL. Only works if the filename is the last 
 	 * part of the URL after the last slash ('/').
 	 * E.g. this function returns 'extlist.txt' from the URL below: 
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
	 * E.g. this function removes 'extlist.txt' from the URL below:
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
	 * Create a local directory at the given path.
	 * Returns false on any kind of error.
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
	
	
	/**
	 * Calculate the SHA-1 checksum of a given list of files.
	 * Returns false on any kind of error.
 	 */
	public static String CalculateFileChecksum(List<String> filenames) throws Exception 
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA1");
	
			for (String filename : filenames)
			{
				FileInputStream fis = new FileInputStream(filename);

				byte[] dataBytes = new byte[1024];
				int nread = 0;
				while ((nread = fis.read(dataBytes)) != -1) 
					md.update(dataBytes, 0, nread);
			}

			byte[] mdbytes = md.digest();
			
			// Convert the bytes to hex format
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) 
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			
			return sb.toString();
		}
		catch (Exception e)
		{
			return null;
		}
	}


	/**
	 * This function removes a list of given files. 
	 */
	private static void RemoveFiles(List<String> filenames)
	{
		for (String filename : filenames)
		{
			try
			{
				File file = new File(filename); 
				file.delete();
			}
			catch (Exception e)
			{
				continue;
			}
		}
	}
}
