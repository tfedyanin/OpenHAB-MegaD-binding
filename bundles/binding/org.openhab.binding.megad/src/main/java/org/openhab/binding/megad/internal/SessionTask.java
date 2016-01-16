package org.openhab.binding.megad.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.megad.MegaDBindingProvider;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;

class SessionTask implements Runnable {
		private static final Pattern getPattern = Pattern.compile("GET /\\?pt=(\\d+)&?(.*) HTTP/1.1");
		private Socket accept;
		Collection<MegaDBindingProvider> providers;
		private final EventPublisher publisher;
		
		public SessionTask(Socket accept, Collection<MegaDBindingProvider> providers, EventPublisher publisher) {
			super();
			this.accept = accept;
			this.providers = providers;
			this.publisher = publisher;
		}
		
		public void run() {
			try {
				BufferedReader acceptReader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
				PrintWriter acceptWriter = new PrintWriter(accept.getOutputStream(), true);
				
				String input, additional = null;
				String[] split = accept.getInetAddress().toString().split("/");
				if(split.length < 1) {
					//TODO
					return;
				}
				String host = split[split.length - 1];
				int pt = -1;
				while((input = acceptReader.readLine()) != null){
					if(input.equals("")){
						break;
					}
					Matcher getMatcher = getPattern.matcher(input);
//					Matcher hostMather = hostPattern.matcher(input);
				
					if(getMatcher.matches()){
						getMatcher.reset();
						getMatcher.find();
						pt = Integer.parseInt(getMatcher.group(1));
						additional = getMatcher.group(2);
					}
//					if(hostMather.matches()){
//						hostMather.reset();
//						hostMather.find();
//						host = hostMather.group(1);
//					}					
				}	
				if((host == null)||(pt == -1))
				{
					return;
				}
//				ArrayList<Object> suitableBindings = new ArrayList<>();
				MegaDBindingConfig suitableBindingsConfig = null;
				String itemName = null;
				
				for(MegaDBindingProvider p : providers){
					Map<String, MegaDBindingConfig> configs = p.getConfigs();
					for(Map.Entry<String, MegaDBindingConfig> entry : configs.entrySet()){
						MegaDBindingConfig bindingConfig = entry.getValue();
						if((bindingConfig.host.equals(host))&&(bindingConfig.pt == pt))
						{
							itemName = entry.getKey();
							suitableBindingsConfig = new MegaDBindingConfig(bindingConfig);
							break;
						}
						
					}
				}
				
				if(suitableBindingsConfig != null){
					System.out.println("Additional == " +additional);
					State state = null;
					if(additional != null && additional.contains("m=1")){
						System.out.println("Additional == OFF" );
						state = createState(suitableBindingsConfig.itemType, "OFF");
					} else {
						System.out.println("Additional == ON" );
						state = createState(suitableBindingsConfig.itemType, "ON");						
					}
					
					//State state = createState(suitableBindingsConfig.itemType, additional != null && additional.contains("m=1") ? "OFF" : "ON" );
					publisher.postUpdate(itemName, state);
					writeOk(acceptWriter);					
				} else {
					writeFail(acceptWriter);
				}			
				
				acceptReader.close();
				acceptWriter.close();
			} catch (IOException e) {
				
			} finally {
				try {
					accept.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
						
		}
		
		private State createState(Class<? extends Item> itemType, String transformedResponse) {
			try {
				if (itemType.isAssignableFrom(NumberItem.class)) {
					return DecimalType.valueOf(transformedResponse);
				} else if (itemType.isAssignableFrom(ContactItem.class)) {
					return OpenClosedType.valueOf(transformedResponse);
				} else if (itemType.isAssignableFrom(SwitchItem.class)) {
					return OnOffType.valueOf(transformedResponse);
				} else if (itemType.isAssignableFrom(RollershutterItem.class)) {
					return PercentType.valueOf(transformedResponse);
				} else if (itemType.isAssignableFrom(DateTimeItem.class)) {
					return DateTimeType.valueOf(transformedResponse);
				} else {
					return StringType.valueOf(transformedResponse);
				}
			} catch (Exception e) {
//				logger.debug("Couldn't create state of type '{}' for value '{}'", itemType, transformedResponse);
				return StringType.valueOf(transformedResponse);
			}
		}
		
		private void writeOk(Writer writer) throws IOException{
			writer.write("HTTP/1.1 200 OK\r\n" + "\r\n\r\n"); 
			writer.flush();					
		}
		
		private void writeFail(Writer writer) throws IOException {
			writer.write("HTTP/1.1 404 Not Found\r\n" + "\r\n\r\n"); 
			writer.flush();			
		}
		
	}