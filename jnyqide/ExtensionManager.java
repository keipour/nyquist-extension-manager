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

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ExtensionManager dialog = new ExtensionManager();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public ExtensionManager() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			table = new JTable();
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setCellSelectionEnabled(false);
			table.setColumnSelectionAllowed(false);
			table.setRowSelectionAllowed(true);
			String header[] = new String[] { "Package", "Ver.", "Date", "Description", "URL" };
			DefaultTableModel dtm = new DefaultTableModel(0, 0)			
			{
				Class[] columnTypes = new Class[] {
						String.class, String.class, String.class, String.class, String.class
				};
				public Class getColumnClass(int columnIndex) {
					return columnTypes[columnIndex];
				}
				boolean[] columnEditables = new boolean[] {
					false, false, false, false, false
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
				getContentPane().add(table, BorderLayout.NORTH);
			}
			getContentPane().add(new JScrollPane(table));
			
			String[] extensions = LoadExtensionData();
			
			for (int i = 0; i < extensions.length; ++i)
			{
				String[] ext = SplitExtensionData(extensions[i], 5);
		        dtm.addRow(new Object[] { ext[0], ext[2], ext[3], ext[4], ext[1] });
			}
		}
		
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton addButton = new JButton("Install");
				addButton.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent arg0) 
					{
						String extDir = System.getenv("NYQEXTPATH");
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
				addButton.setActionCommand("Add");
				buttonPane.add(addButton);
				getRootPane().setDefaultButton(addButton);
			}
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
		}
	}
	
	private String[] ExtractOtherFilesFromSAL(String fileContent)
	{
		String[] lines = fileContent.split(System.getProperty("line.separator"));
		List<String> fileNames = new ArrayList<String>();
		for (int i = 0; i < lines.length; ++i)
		{
			String line = lines[i].trim();
			if (line.isEmpty()) continue;
			if (line.substring(0, 2).equals(";;") == false) break;
			try
			{
				line = line.substring(2).trim();
				String searchTerm = "Additional-File:";
				if (line.substring(0, searchTerm.length()).equalsIgnoreCase(searchTerm) == true)
					fileNames.add(line.substring(searchTerm.length()).trim());
			}
			catch (Exception e)
			{
				continue;
			}
		}
		String[] result = new String[fileNames.size()];
		result = fileNames.toArray(result);

		return result;
	}
	
	String[] SplitExtensionData(String line, int numOfCells)
	{
		String[] result = new String[numOfCells];
		result = line.split(",", numOfCells);
		for (int i = 0; i < result.length; ++i)
			result[i] = result[i].trim();
		return result;
	}
		
	String[] LoadExtensionData()
	{
		String link = "https://raw.githubusercontent.com/keipour/nyquist-extensions/master/extlist.txt";
		return ReadFromURL(link).split(System.getProperty("line.separator"));
	}
	
	private String ReadFromURL(String link)
	{
		try{
		    URL url = new URL(link);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			Map<String, List<String>> header = http.getHeaderFields();

			InputStream stream = http.getInputStream();
			return GetStringFromStream(stream);
		} 
		catch(Exception e)
		{
			return null;
		}
	}
	
	private String GetStringFromStream(InputStream stream) throws IOException 
	{
		if (stream != null) {
			Writer writer = new StringWriter();
 
			char[] buffer = new char[2048];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
				int counter;
				while ((counter = reader.read(buffer)) != -1) 
					writer.write(buffer, 0, counter);
			} 
			finally 
			{
				stream.close();
			}
			return writer.toString();
		} 
		else 
		{
			return null;
		}
	}
	
	private String SaveFromURL(String link, String dir)
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
		}
		catch (Exception e)
		{ 
			JOptionPane.showMessageDialog(contentPanel, "Error writing file to " + filepath + "'!"); 
		}
		
		return fileContent;
	}
	
	private String ExtractFilenameFromGithubURL(String link)
	{
		return link.substring(link.lastIndexOf('/') + 1);
	}
	
	private String DirFromGithubURL(String link)
	{
		return link.substring(0, link.lastIndexOf('/') + 1);
	}

	private static void MakeDirectory(String path)
	{
		File theDir = new File(path);
		
		// if the directory does not exist, create it
		if (!theDir.exists()) 
		{
		    try
		    {
		        theDir.mkdir();
		    } 
		    catch(SecurityException se) {   }        
		}
	}
}
