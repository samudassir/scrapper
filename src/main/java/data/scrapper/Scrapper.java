package data.scrapper;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlBlockQuote;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlStrong;

@SuppressWarnings("unchecked")
public class Scrapper {
	private static String xpath1 = "//div[@class='forumdata']";
	private static String xpath1_1 = ".//h2[@class='forumtitle']/a";
	private static String xpath2 = ".//h3[@class='threadtitle']";
	private static String xpath2_1 = ".//a[@class='title']";
	private static String xpath3 = ".//title";
	private static String xpath3_1 = ".//span[@class='date']";
	private static String xpath3_2 = ".//div[@class='postdetails']";
	private static String xpath3_3 = ".//blockquote[@class='postcontent restore ']";
	private static String xpath3_3_1 = ".//blockquote[@class='signature restore']";
	private static String xpath3_4 = ".//strong";
	
	private static String fileName = "cancer_data.csv";
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";

	final static String FILE_HEADER = "postedDate,message,profile,userId";
	static FileWriter fileWriter = null;


	public static void main(String[] args) {
		try {
		fileWriter = new FileWriter(fileName);
		fileWriter.append(FILE_HEADER.toString());
		fileWriter.append(NEW_LINE_SEPARATOR);
		}catch(Exception e) {
			e.printStackTrace();
		}

		scrapeCancerForum("realTime");
	}
	
	public static void scrapeCancerForum(String job) {
		String baseUrl = "https://www.cancerforums.net/";

		WebClient client = new WebClient();

		client.getOptions().setCssEnabled(false);
		client.getOptions().setJavaScriptEnabled(false);
			try {
				HtmlPage page = getHtmlPageContent(client, baseUrl);
				buildMessage(job, baseUrl, client, retrieveSecondPageURLs(baseUrl, client, retrieveLandingPageURLs(page)));
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	private static List<String> retrieveLandingPageURLs(HtmlPage page) {
		List<String> landingPageURLs = new ArrayList<String>();
		List<HtmlElement> topElement = getHtmlElement(page, xpath1);
		for (HtmlElement htmlElement : topElement) {
			HtmlAnchor itemAnchor = getFirstAnchorByXpath(xpath1_1, htmlElement);
			String URI = itemAnchor.getHrefAttribute();
			if (!(URI.contains("Policies") || URI.contains("FAQ"))) {
				landingPageURLs.add(URI);
			}
		}
		return landingPageURLs;
	}

	private static List<String> retrieveSecondPageURLs(String baseURL, WebClient client, List<String> landingPageURLs)
			throws IOException, MalformedURLException {
		List<String> secondPageURLs = new ArrayList<String>();
		for (String uri : landingPageURLs.subList(0, 1)) {
			String url = baseURL + uri;
			HtmlPage innerPage = getHtmlPageContent(client, url);
			List<HtmlElement> secondElement = getHtmlElement(innerPage, xpath2);
			for (HtmlElement htmlElement : secondElement) {
				HtmlAnchor itemAnchor = getFirstAnchorByXpath(xpath2_1, htmlElement);
				secondPageURLs.add(itemAnchor.getHrefAttribute());
			}
		}
		return secondPageURLs;
	}

	private static void buildMessage(String job, String baseUrl, WebClient client, List<String> secondPageURLs)
			throws IOException, MalformedURLException {
		for (int j = 0; j < secondPageURLs.size(); j++) {
			String uri = secondPageURLs.get(j);
			String url = baseUrl + uri;
			HtmlPage contentPage = getHtmlPageContent(client, url);
			HtmlElement titleElement = getFirstElementByXpath(xpath3, contentPage);
			String title = titleElement.asText();
			System.out.println("title ========" + titleElement.asText());
			StringBuffer sb = new StringBuffer();
			sb.append(uri);
			sb.append("-");
			sb.append(title);
			String messageId = sb.toString().replace("threads/", "").replace(" ", "");
			List<HtmlElement> dateElement = getHtmlElement(contentPage, xpath3_1);
			List<HtmlElement> innerHtmlElement = getHtmlElement(contentPage, xpath3_2);
			
			if (!title.contains("Forum Policies"))
				for (int i = 0; i < innerHtmlElement.size(); i++) {
					String date = dateElement.get(i).asText();
					if(job.equalsIgnoreCase("realTime") || date.equalsIgnoreCase("Today") || date.equalsIgnoreCase("Yesterday")){
						HtmlElement htmlElement2 = innerHtmlElement.get(i);
						List<HtmlBlockQuote> itemBlock = getBlockQuoteByXPath(xpath3_3, htmlElement2);
						List<HtmlBlockQuote> itemSignatureBlock = getBlockQuoteByXPath(xpath3_3_1, htmlElement2);
						List<HtmlStrong> userInfo = getUserInfoByXPath(xpath3_4, htmlElement2);
					}
				}
		}
	}
	

	private static Map<String, Object> populateEvents(ZonedDateTime date, List<HtmlBlockQuote> itemBlock,
			List<HtmlBlockQuote> itemSignatureBlock, List<HtmlStrong> userInfo) {
		Map<String, Object> messageInfo = new HashMap<String, Object>();
		messageInfo.put("postedDate", date);

		for (HtmlBlockQuote htmlBlockQuote : itemBlock) {
			String messageVal = htmlBlockQuote.asText();
			messageInfo.put("message", messageVal);
		}
		for (HtmlBlockQuote htmlSignatureBlockQuote : itemSignatureBlock) {
			String profileInfo = htmlSignatureBlockQuote.asText();
			messageInfo.put("profile", profileInfo);
		}
		for (HtmlStrong htmlStrong : userInfo) {
			String userId = htmlStrong.asText();
			messageInfo.put("userId", userId);
		}
		System.out.println(messageInfo);
		buildCSVRecords(messageInfo, COMMA_DELIMITER, NEW_LINE_SEPARATOR, fileWriter);
		return messageInfo;
	}
	
	private static void buildCSVRecords(Map<String, Object> messageInfo, final String COMMA_DELIMITER,
			final String NEW_LINE_SEPARATOR, FileWriter fileWriter){
		try {
		//Write a new student object list to the CSV file
			fileWriter.append(String.valueOf(messageInfo.get("postedDate")));
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(String.valueOf(messageInfo.get("message")).replaceAll(",", "").replaceAll("\n", ""));
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(String.valueOf(messageInfo.get("profile")).replaceAll(",", "").replaceAll("\n", ""));
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(String.valueOf(messageInfo.get("userId")));
			fileWriter.append(NEW_LINE_SEPARATOR);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static ZonedDateTime calculateLocalDateTime(String dateTime) {
		System.out.println(dateTime+"   <<<<<<<<<<<<<<<<<<<<<<  dateTime");
		LocalDateTime localDateTime =null;
		LocalDate localDate = null;
		String dateTimeArr[] = dateTime.split(",");
		String date = dateTimeArr[0];
		int month = 0;
		int day = 0;
		int year = 0;
		if("Today".equals(date)) {
			localDate = LocalDate.now()	;
			month = localDate.getMonthValue();
			day = localDate.getDayOfMonth();
			year = localDate.getYear();
		}else if("Yesterday".equals(date)) {
			localDate = LocalDate.now().minusDays(1)	;
			month = localDate.getMonthValue();
			day = localDate.getDayOfMonth();
			year = localDate.getYear();
		}else {
		String dateArr[] = date.split("-");
		month = Integer.parseInt(dateArr[0]);
		day = Integer.parseInt(dateArr[1]);
		year = Integer.parseInt(dateArr[2]);
		}
		String time = dateTimeArr[1];
		
		if(time.contains("PM")) {
			String timeArr[] = time.split(":");
			int hr = Integer.parseInt(timeArr[0].trim())+11;
			int min = Integer.parseInt(timeArr[1].substring(0, 2));
			localDateTime = LocalDateTime.of(year, month, day, hr, min);
		}else {
			String timeArr[] = time.split(":");
			int hr = Integer.parseInt(timeArr[0].trim());
			int min = Integer.parseInt(timeArr[1].substring(0, 2));
			localDateTime = LocalDateTime.of(year, month, day, hr, min);
		}
		ZoneId zoneId = ZoneId.of("America/Los_Angeles");
		return localDateTime.atZone(zoneId);
	}


	private static HtmlPage getHtmlPageContent(WebClient client, String url) throws IOException, MalformedURLException {
		HtmlPage contentPage = client.getPage(url);
		return contentPage;
	}

	private static List<HtmlStrong> getUserInfoByXPath(String xpath3_4, HtmlElement innerHtmlElement) {
		List<HtmlStrong> userInfo = (List<HtmlStrong>) innerHtmlElement.getByXPath(xpath3_4);
		return userInfo;
	}

	private static List<HtmlBlockQuote> getBlockQuoteByXPath(String xpath3_3, HtmlElement innerHtmlElement) {
		List<HtmlBlockQuote> itemBlock = (List<HtmlBlockQuote>) innerHtmlElement.getByXPath(xpath3_3);
		return itemBlock;
	}

	private static HtmlElement getFirstElementByXpath(String xpath3, HtmlPage contentPage) {
		HtmlElement titleElement = (HtmlElement) contentPage.getFirstByXPath(xpath3);
		return titleElement;
	}

	private static HtmlAnchor getFirstAnchorByXpath(String xpath1_1, HtmlElement htmlItem) {
		HtmlAnchor itemAnchor = ((HtmlAnchor) htmlItem.getFirstByXPath(xpath1_1));
		return itemAnchor;
	}

	private static List<HtmlElement> getHtmlElement(HtmlPage page, String xpath1) {
		List<HtmlElement> topElement = (List<HtmlElement>) page.getByXPath(xpath1);
		return topElement;
	}
}
