package com.pi4j.util;

/*
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Pi4J
 * PROJECT       :  Pi4J :: Java Library (Core)
 * FILENAME      :  NativeLibraryLoader.java  
 * 
 * This file is part of the Pi4J project. More information about 
 * this project can be found here:  http://www.pi4j.com/
 * **********************************************************************
 * %%
 * Copyright (C) 2012 Pi4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeLibraryLoader
{
    private static List<String> loadedLibraries = null;

    public static synchronized void load(String libraryName)
    {
        load(libraryName, null);
    }

    public static synchronized void load(String libraryName, String fileName)
    {
        // create instance if null
        if (loadedLibraries == null)
            loadedLibraries = Collections.synchronizedList(new ArrayList<String>());

        // first, make sure that this library has not already been previously loaded
        if (!loadedLibraries.contains(libraryName))
        {
            // assume library loaded successfully, add to tracking collection
            loadedLibraries.add(libraryName);

            try
            {
                // attempt to load the native library from the system classpath loader
                System.loadLibrary(libraryName);
            }
            catch (UnsatisfiedLinkError e)
            {
                // if a filename was not provided, then throw exception
                if (fileName == null)
                {
                    // library load failed, remove from tracking collection
                    loadedLibraries.remove(libraryName);

                    throw e;
                }

                try
                {
                    // attempt to get the native library from the JAR file in the 'lib'
                    // directory
                    URL resourceUrl = NativeLibraryLoader.class.getResource("/lib/" + fileName);

                    // create a 1Kb read buffer
                    byte[] buffer = new byte[1024];
                    int byteCount = 0;

                    // open the resource file stream
                    InputStream inputStream = resourceUrl.openStream();

                    // get the system temporary directory path
                    File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
                    
                    // check to see if the temporary path exists
                    if(!tempDirectory.exists())
                    {
                        // TODO: change this to a logger instead of sysout
                        System.out.println("WARNING:  The Java system path [" + tempDirectory.getAbsolutePath() + "] does not exist.");
                        
                        // instead of the system defined temporary path, let just use the application path
                        tempDirectory = new File("");
                    }
                    
                    // create a temporary file to copy the native library content to
                    File tempFile = new File(tempDirectory.getAbsolutePath() + "/" + fileName);
                    
                    // make sure that this temporary file does not exist; if it does then delete it
                    if(tempFile.exists())
                    {
                        // TODO: change this to a logger instead of sysout
                        System.out.println("WARNING:  The temporary file already exists [" + tempFile.getAbsolutePath() + "]; attempting to delete it now.");                        
                        tempFile.delete();
                    }
                    
                    OutputStream outputStream = null;
                    
                    try
                    {
                        // create the new file
                        outputStream = new FileOutputStream(tempFile);
                    }
                    catch(FileNotFoundException fnfe)
                    {
                        // TODO: change this to a logger instead of sysout
                        System.out.println("ERROR:  The temporary file [" + tempFile.getAbsolutePath() + "] cannot be created; it is a directory, not a file.");
                        fnfe.printStackTrace();
                        throw(fnfe);
                    }
                    catch(SecurityException se)
                    {
                        // TODO: change this to a logger instead of sysout
                        System.out.println("ERROR:  The temporary file [" + tempFile.getAbsolutePath() + "] cannot be created; a security exception was detected. " + se.getMessage());
                        se.printStackTrace();
                        throw(se);
                    }
                    
                    if(outputStream != null)
                    {
                        try
                        {
                            // ensure that this temporary file is removed when the program exits
                            tempFile.deleteOnExit();
                        }
                        catch(SecurityException dse)
                        {
                            // TODO: change this to a logger instead of sysout
                            System.out.println("ERROR:  The temporary file [" + tempFile.getAbsolutePath() + "] cannot be flagged for removal on program termination; a security exception was detected. " + dse.getMessage());
                            dse.printStackTrace();
                        }
    
                        try
                        {
                            // copy the library file content
                            while ((byteCount = inputStream.read(buffer)) >= 0)
                                outputStream.write(buffer, 0, byteCount);
                            
                            // flush all write data from stream 
                            outputStream.flush();
                            
                            // close the output stream
                            outputStream.close();                            
                        }
                        catch(IOException ioe)
                        {
                            // TODO: change this to a logger instead of sysout
                            System.out.println("ERROR:  The temporary file [" + tempFile.getAbsolutePath() + "] could not be written to; an IO exception was detected. " + ioe.getMessage());
                            ioe.printStackTrace();
                            throw(ioe);                            
                        }
    
                        // close the input stream
                        inputStream.close();
    
                        try
                        {
                            // load the new temporary library file
                            System.load(tempFile.getAbsolutePath());
                        }
                        catch(SecurityException libse)
                        {
                            // TODO: change this to a logger instead of sysout
                            System.out.println("ERROR:  The native library file [" + tempFile.getAbsolutePath() + "] could not be loaded due to a security exception; " + libse.getMessage());
                            libse.printStackTrace();
                            throw(libse);                            
                        }
                        catch(UnsatisfiedLinkError ule)
                        {
                            // TODO: change this to a logger instead of sysout
                            System.out.println("ERROR:  The native library file [" + tempFile.getAbsolutePath() + "] could not be loaded due to an unsatisfied link error; " + ule.getMessage());
                            ule.printStackTrace();
                            throw(ule);                                                        
                        }
                        catch(NullPointerException npe)
                        {
                            // TODO: change this to a logger instead of sysout
                            System.out.println("ERROR:  The native library file [" + tempFile.getAbsolutePath() + "] could not be loaded due to an invalid file path; " + npe.getMessage());
                            npe.printStackTrace();
                            throw(npe);                                                        
                        }
                    }                    
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();

                    // library load failed, remove from tracking collection
                    loadedLibraries.remove(libraryName);

                    // TODO: change this to a logger instead of sysout
                    System.out.println("ERROR:  The native library [" + libraryName + " : " + fileName + "] could not found in the JVM library path nor could it be loaded from the embedded JAR resource file; you may need to explicitly define the library path '-Djava.library.path' where this native library can be found.");
                }
            }
        }
    }
}
