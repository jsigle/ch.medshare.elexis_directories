/*******************************************************************************
 * Portions copyright (c) 2010, 2012 Jörg Sigle www.jsigle.com and portions copyright (c) 2007, medshare and Elexis
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle - 20120712-20120713 www.jsigle.com
 *    			 On about 2012-07-11 (or 2012-07-04?), tel.local.ch vastly changed their delivery formats again.
 *    			   This rendered this plugin completely dysfunctional.
 *    		     Changed http get implementation to use a connection and userAgent setting,
 *     			   otherwise, WAP content would be served that did not directly reveal the desired content.
 *     			 Reviewed, made functional again, and improved pre-processing and parsing steps.
 *               Added a few features regarding filling of the Zusatz field from the list format,
 *                 among others: interpretation of the Categories field unnumbered list entries
 *                 into the field Zusatz, as long as no better information is obtained from a Details entry.
 *               When a single entry is double clicked now in a result list,
 *                 additional / overwriting content is now truly filled in from the resulting served Details entry. 
 *               URIencoded e-mail addresses are now decoded and stripped from excess wrapping characters.
 *               
 *
 *    J. Sigle - 20101213-20101217 www.jsigle.com
 *    			 Hopefully, all of my changes are marked by a comment that has "js" in it.
 *    			 Adoption of processing of results in ADR_LIST format to changed html content,
 *                 as recently introduced by http://tel.local.ch.
 *               Filling of poBox (and some other fields) made conditional:
 *                 The target is now initialized as an empty string, and only filled from the
 *                 processed html when moveTo("SomeRespectiveTag") was successful.
 *                 Otherwise, some garbage could be read (or persist) in(to) the target
 *                 variable - and, in the case of poBox, if zusatz was empty, that garbage
 *                 would appear up there. Effectively, the user had to clean zusatz
 *                 more often than not e.g. when the address of a health insurance company
 *                 was obtained.
 *               Enhanced removeDirt() to also remove leading and trailing blanks/spaces.
 *                 I found addresses with trailing blanks in the street address field quite often.
 *               Some comments added with regard to program functionality.
 *               Some debug/monitoring output added in internal version only;
 *                 commented out again for published version: //System.out.print("jsdebug: ...           
 *                 
 *    			 Suggestion regarding getVornameNachname():
 *                 Maybe the first-name/last-name split should be changed,
 *    			   or an interactive selection of the split point be provided.
 *                 See new comment before the function header.
 *                 
 *               Suggestion regarding zusatz:
 *                 If a title like Dr. med., PD Dr. med., Prof. is encountered,
 *                 split it off and put it directly into the title field.
 *                 (We use zusatz for a Facharztbezeichnung like Innere Medizin FMH,
 *                  and title for a Titel like Dr. med. etc.)
 *    
 *               Suggestion regarding second level search request:
 *                 Problem: When I first search for anne müller in bern,
 *                 and thereafter double click on the first entry, I get data
 *                 in the add-contact-dialog that does NOT include her title (role).
 *                 (with java code revised by myself, which works for other
 *                  müllers returned in the first result list, including their titles).
 *                  
 *                 Similar problem: Looking for Henzi in Bern finds a whole list of results.
 *                 Double clicking the last (Stefan Henzi) returns another list.
 *                 Some of the entries have the title Dr. med. and some don't -
 *                 Elexis does NOT show the second list for further selection,
 *                 and it apparently evaluates one that doesn't have the title.
 *                 
 *                 Not similar - but related problem: Looking for Hamacher in Bern
 *                 returns two entries. Double click on Jürg Hamacher in the tel.local.ch
 *                 page in the WWW browser returns a single entry. This detail entry
 *                 (but not the previously shown list entry) includes the title and
 *                 the e-mail address. Neither is transferred to Elexis when double
 *                 clicking on the second list entry.
 *                 But when I search for Jürg Hamacher immediately, the single detailed
 *                 entry is found immediately, and all information from there transferred.  
 *                     
 *                     
 *                 I'm somewhat unsure whether my assumption regarding what happens
 *                 are correct here (would need to further review the program).
 *                 I currently guess that:
 *                 
 *                 If a user clicks on one entry from the list returned by an initial search,
 *                 then feed the second level search (which will feed the new kontakt entry dialog)
 *                 with both the name AND the address returned from the first level search.
 *                 
 *                 (a) The results from the second level search, may be processed, but are
 *                     NOT really used by Elexis. I.e. I see debug output from inside
 *                     extractKontakt(), but I don't see any effects (i.e. changed variable
 *                     content) in the Elexis Kontakt dialog.
 *                     (The Jürg Hamacher example)
 *                     
 *                 (b) Supplying ONLY the name, will not suffice to get a single-entry result
 *                     e.g. for Anne Müller in Bern, so her title which is available only in the
 *                     detailed result output will be missed.
 *                     (The Anna Müller Stefan Henzi examples)
 *                
 *                 Moreover, it is (maybe) shere luck that in this case,
 *                 we have only ONE Anne Müller in Bern in the result list - 
 *                 and all others have some additional names. Otherwise, I'm unsure
 *                 whether a result list containing e.g. TWO entries for Max Muster,
 *                 would return the correct one for either case in the second level search...
 *                 (The Stefan Henzi examples, I guess, *might* illustrate just that.)
 *
 *                 I have followed this through to WeiseSeitenSearchForm.java
 *                 openKontaktDialog() where the information from the list_format entry
 *                 (i.e. with empty zusatz, exactly the entry at which we double clicked) is supplied.
 *                 
 *                 I've also reviewed open.dialog() one step further - but then it becomes too
 *                 much for me for today. From my debugging output, I can clearly see that
 *                 (for the Jürg Hamacher example):
 *                 
 *                 *after* openKontaktDialog() is called,
 *                 there occurs another call of extractKontakte() (from where?!)
 *                 which calls extractKontakt(),
 *                 which returns all information from the detailed information (),
 *                 which is added as part of a new kontakte.entry at the end of the extractKontakt() loop,
 *                 and apparently ignored thereafter (why that?)
 *                 
 *                 I'm unsure whether this lasts search/search result processing should not
 *                 better occur *before* the dialog is opened, and its information used to
 *                 feed the dialog. But take care; that multiple dialogs may be opened if
 *                 multiple contacts are selected on the first list, so they all must be
 *                 fed with individual new searches, and the original contact list may not
 *                 be forgotten until the last dialog window so generated has been closed...
 *                 
 *                 Please look at my extensive Anne Müller related comments below;
 *                 and please note, that Anne Müller's "Zusatz" is not lost because
 *                 I changed some zusatz related lines, but because the second level
 *                 search request apparently returns a list_format result (again),
 *                 which does NOT have a title entry for her and/or because when a
 *                 detailed_format result is returned in a second level search,
 *                 it is processed, but its results are not honoured.
 *                 
 *                 Please review the output of tel.local.ch for all entries on the
 *                 first anne müller bern search result, and what happens with the
 *                 various titles. Some work, some don't.
 *                 Please also review the Stefan Henzi example case.
 *                 
 *                 You can easily switch on my debug output:
 *                 look for: //Anne Müller case debug output
 *                 in DirectoriesContentParser.java (this file)
 *                 and WeisseSeitenSearchView.java
 *                 Or just uncomment all occurences of "//System.out.print("jsdebug:"
 *                 in these files (except the one in the line above) with find/replace.
 *                 
 *                 You can also set the variable zusatz to a fixed value in either
 *                 extractKontakt() or extractListKontakt() functions and see what is used,
 *                 and what is ignored.
 *
 *                 Sorry - for myself, I just don't have the time to review that
 *                 problem in more detail by now today; I also think it's of secondary
 *                 importance compared to the restoration of the first level search
 *                 function in general.
 *
 *    M. Imhof - initial implementation
 *    
 * $Id: DirectoriesContentParser.java 5277 2009-05-05 19:00:19Z tschaller $
 *******************************************************************************/

package ch.medshare.elexis.directories;

import java.io.IOException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.io.UnsupportedEncodingException;		//20120713js
import java.net.URLDecoder;							//20120713js

/**
 * 
 * @author jsigle (comment and 20101213, 20120712 update only)
 * 
 *         The service http://tel.local.ch provides a user-interface for WWW
 *         browsers with a lot of additional content around the desired
 *         address/contact information. The address/contact information is
 *         extracted from this material and returned in variable fields suitable
 *         for further usage by Elexis.
 * 
 *         If a search request returns multiple results, these appear in (what
 *         we call) "ADR_LIST" format. If a search request returns exactly one
 *         result (or one entry from the list is clicked at in the WWW browser),
 *         the result appears in (what we call) "ADR_DETAIL" format.
 * 
 *         By 12/2010, a change in the output format of tel.local.ch required
 *         new marker strings for processing. The processing of a result in
 *         ADR_DETAIL format continued to work, But the processing of a result
 *         in ADR_LIST format would deliver an empty result.
 * 
 *         By 2012-07-11, the plugin stopped working again.
 * 
 *         Please note that there is relaunch related info on
 *         www.directoriesdata.ch www.local.ch shall apparently be split into a
 *         free consumer site, and a commercial counterpart.
 * 
 *         Also note that neither &mode=text nor &range=all do currently change
 *         the result which I observe.
 * 
 * 
 */
public class DirectoriesContentParser extends HtmlParser {

	// 20101213js - 20101217js - 20120712js
	//
	// Original code before 20101213:
	//private static final String ADR_LIST_TAG = "class=\"vcard searchResult resrowclr"; //$NON-NLS-1$
	//private static final String ADR_DETAIL_TAG = "<div class=\"resrowclr";; //$NON-NLS-1$
	//
	// I tried several alternative markers with different results;
	// see additional notes in the non-published interim version dated 20101213.
	//
	// New code, after 20101213:
	//private static final String ADR_LIST_TAG = "class=\"searchResult phonebook"; //$NON-NLS-1$
	//private static final String ADR_DETAIL_TAG = "<div class=\"resrowclr";; //$NON-NLS-1$
	//
	// New code, after 20120712:
	//
	// What I thought when I started today, and what probably *should*
	// preferrably be true:
	// It is not important that the marker strings occur immediately before the
	// interesting content,
	// but they should reliably occur only once before the first occurence of a
	// tag identifying interesting content.
	// wget receives results that are stored with single quotation marks in a
	// file.
	// the elexis plugin currently appears to receive a result for wap devices
	// ?!?!
	//private static final String ADR_LIST_TAG = "<div class=\'listing-type"; //$NON-NLS-1$
	//private static final String ADR_DETAIL_TAG = "<div class=\'inner-box";; //$NON-NLS-1$
	//
	// What I learned today and what *really* is true:
	// Nay, apparently, it is (now, maybe since my last revision) important that
	// we set as ADR_LIST_TAG something that is close to each list entry in the
	// list format html code
	// and as ADR_DETAIL_TAG something that occurs immediately before the
	// family_name/christian_name in the details format html code.
	//
	// Please note that some class=... tags come with single quotation marks,
	// others with double quotation marks, and what this plugin processes is not identical
	// to what mozilla would save in a file with this respect.
	//
	private static final String ADR_LISTENTRY_TAG = "<div class='listing'"; //$NON-NLS-1$
	private static final String ADR_SINGLEDETAILENTRY_TAG = "<div class='details'";; //$NON-NLS-1$

	public DirectoriesContentParser(String htmlText) {
		super(htmlText);
	}

	/**
	 * Retourniert String in umgekehrter Reihenfolge
	 */
	private String reverseString(String text) {
		if (text == null) {
			return "";
		}
		String reversed = "";
		for (char c : text.toCharArray()) {
			reversed = c + reversed;
		}
		return reversed;
	}

	/**
	 * Comment added: 201012130058js This splits the provided string at the
	 * first contained space. This is not optimal for all cases: Persons may
	 * have multiple given names / christian names, and they will very often be
	 * separated just by spaces. I actually observed in real life usage that a
	 * second given name went to the "Name"="Nachname"="Family name" field
	 * together with the true family name. It might be better to split the name
	 * at the *last* contained space, because multiple family names are usually
	 * linked by a dash (-), rather than separated by a space (this is my
	 * personal impression). However, I haven't changed the code so far.
	 */
	private String[] getVornameNachname(String text) {
		String vorname = ""; //$NON-NLS-1$
		String nachname = text;
		int nameEndIndex = text.trim().indexOf(" "); //$NON-NLS-1$
		if (nameEndIndex > 0) {
			vorname = text.trim().substring(nameEndIndex).trim();
			nachname = text.trim().substring(0, nameEndIndex).trim();
		}
		return new String[] { vorname, nachname };
	}

	private String removeDirt(String text) {
		// 20101217js
		// remove leading and trailing whitespace characters
		text = text.replaceAll("^+\\s", "");
		text = text.replaceAll("\\s+$", "");

		return text.replace("<span class=\"highlight\">", "").replace("</span>", "");
	}

	/**
	 * Informationen zur Suche werden extrahiert.
	 * 
	 * 20101213js added comments Bsp: (valid before 2010-12-xx)
	 * 
	 * <div class="summary"> <strong>23</strong> Treffer für <strong
	 * class="what">müller hans</strong> in <strong class="where">bern</strong>
	 * <div id="printlink" .... <span class="spacer">&nbsp;</span> <a
	 * href="http://tel.local.ch/de/">Neue Suche</a> </div>
	 * 
	 * Bsp: (valid after 2010-12-13, linebreaks added for clarity)
	 * 
	 * <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	 * "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> <html
	 * lang="de" xml:lang="de"> <head><meta http-equiv="Content-Type"
	 * content="text/html; charset=UTF-8" /> <title>meier in bern - 965 Treffer
	 * auf local.ch</title> <link rel="stylesheet" ...
	 * 
	 */
	public String getSearchInfo() {
		reset();

		System.out.print("jsdebug: DirectoriesContentParser.java: getSearchInfo() running...\n");
		System.out.print("jsdebug: Beginning of substrate: <"+extract("<",">")+"...\n");

		// 20101213js
		// The elexis Original line here was:
		// String searchInfoText = extract("<div class=\"summary\">",
		// "<div id=\"printlink\"");

		// tel.local.ch however have changed the "<div id=" into "<a id=";
		// I'm actually unsure whether we can rely on this; but as the thing is
		// called
		// print*link* we might assume that it's quite plausible to expect it in
		// an <a ...> tag.

		// Trying it out...
		// OK this returns SOMETHING, but much much more than what we want...:
		// String searchInfoText = extract("<div class=\"summary\">",
		// "<a id=\"printlink\"");

		// OK. I could probably find a better marker string, but actually, the
		// thing that we want
		// is much more easily given in the <title> of the page, so why not just
		// evaluate that...
		// :-)
		String searchInfoText = extract("<title>", "</title>");
		// Works like a charm, result is beautiful :-) 201012130217js
		// N.B.: You might want to remove some postprocessing
		// searchInfoText.replace()
		// which should have become obsolete by processing the <title> content;
		// see corresponding comment a few lines below.

		// 20120712js: This still works today; no changes required at all :-)

		if (searchInfoText == null) {
			return "";//$NON-NLS-1$
		}

		System.out.print("jsdebug: DirectoriesContentParser.java: getSearchInfo(): searchInfoText != null\n");
		System.out.print("jsdebug: DirectoriesContentParser.java: getSearchInfo(): \"" + searchInfoText + "\"\n\n");

		// 20101217js
		// In the updated version processing the <title> tag content,
		// we probably would not need the following replacements any more.
		// I leave them in for now, just in case someone reverts to processing
		// html body code.
		return searchInfoText.replace("<strong class=\"what\">", "")
				.replace("<strong class=\"where\">", "").replace("<strong>", "").replace("</strong>", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * 20101213js:
	 * 
	 * Extrahiert Informationen aus dem retournierten Html. Anhand der <div
	 * class="xxx"> kann entschieden werden, ob es sich um eine Liste oder einen
	 * Detaileintrag (mit Telefon handelt).
	 * 
	 * Detaileinträge: "adrNameDetLev0", "adrNameDetLev1", "adrNameDetLev3" Nur
	 * Detaileintrag "adrNameDetLev2" darf nicht extrahiert werden
	 * 
	 * Listeinträge: "adrListLev0", "adrListLev1", "adrListLev3" Nur Listeintrag
	 * "adrListLev0Cat" darf nicht extrahiert werden
	 * 
	 * 20120712js:
	 * 
	 * (This has possibly changed.)
	 */
	public List<KontaktEntry> extractKontakte() throws IOException {
		reset();

		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte() running...\n");
		System.out.print("jsdebug: Beginning of substrate: <"+extract("<",">")+"...\n");

		List<KontaktEntry> kontakte = new Vector<KontaktEntry>();

		int listIndex = getNextPos(ADR_LISTENTRY_TAG);
		int detailIndex = getNextPos(ADR_SINGLEDETAILENTRY_TAG);

		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte() initial values of...\n");
		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte().listIndex: " + listIndex + "\n");
		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte().detailIndex: " + detailIndex + "\n");

		while (listIndex > 0 || detailIndex > 0) {
			KontaktEntry entry = null;

			System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte() intraloop values of...\n");
			System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte().listIndex: " + listIndex + "\n");
			System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakte().detailIndex: " + detailIndex + "\n");

			if (detailIndex < 0 || (listIndex >= 0 && listIndex < detailIndex)) {
				// Parsing Liste
				System.out.print("jsdebug: DirectoriesContentParser.java: Parsing Liste:\n");
				entry = extractListKontakt();
			} else if (listIndex < 0 || (detailIndex >= 0 && detailIndex < listIndex)) {
				// Parsing Einzeladresse
				System.out.print("jsdebug: DirectoriesContentParser.java: Parsing Einzeladresse:\n");
				entry = extractKontakt();
			}

			if (entry != null) {
				System.out.print("jsdebug: DirectoriesContentParser.java: entry: " + entry.toString() + "\n");
			} else {
				System.out.print("jsdebug: DirectoriesContentParser.java: entry: NULL\n");
			}

			if (entry != null) {
				kontakte.add(entry);

				// 20101217js, 20120712js
				// Anne Müller case debug output:
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Name:     " + entry.getName().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Vorname:  " + entry.getVorname().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Zusatz:   " + entry.getZusatz().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Adresse:  " + entry.getAdresse().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Plz :     " + entry.getPlz().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Ort :     " + entry.getOrt().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Telefon : " + entry.getTelefon().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Fax :     " + entry.getFax().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.Email :   " + entry.getEmail().toString() + "\n");
				System.out.print("jsdebug: extractKontakte() kontakte added entry.isDetail: " + entry.isDetail() + "\n");
				System.out.print("jsdebug: \n");
			}
			listIndex = getNextPos(ADR_LISTENTRY_TAG);
			detailIndex = getNextPos(ADR_SINGLEDETAILENTRY_TAG);
		}

		return kontakte;
	}

	/**
	 * Extrahiert einen Kontakt aus einem Listeintrag
	 * 
	 * Bsp: (valid before 2010-12-xx)
	 * 
	 * <div id="te_ojUHu3vXsUWJbXidz2_sRQ"
	 * onmouseover="lcl.search.onEntryHover(this)" onclick=
	 * "if (typeof(lcl.search) != 'undefined') { lcl.search.navigateTo(event, 'http://tel.local.ch/de/d/ILwo-yKRTlguXS4TFuVPuA?what=Meier&start=3'); }"
	 * class="vcard searchResult resrowclr_yellow mappable"> <div
	 * class="imgbox"> <a href=
	 * "http://tel.local.ch/de/d/ILwo-yKRTlguXS4TFuVPuA?what=Meier&amp;start=3">
	 * <img xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	 * src="http://s.staticlocal.ch/images/pois/na/blue1.png"
	 * alt="Dieser Eintrag kann auf der Karte angezeigt werden" height="26"
	 * width="27" /> </a> </div> <div class="entrybox"> <h4>
	 * <span class="category" title="Garage"> Garage </span> <br>
	 * <a class="fn" href=
	 * "http://tel.local.ch/de/d/ILwo-yKRTlguXS4TFuVPuA?what=Meier&amp;start=3">
	 * Autocenter <span class="highlight">Meier</span> AG </a> </br></h4>
	 * <p * class="bold phoneNumber">
	 * <span class="label">Tel.</span> <span class="tel"> <a class="phonenr"
	 * href="callto://+41627234359"> 062 723 43 59 </a> </span>
	 * </p>
	 * <p class="adr">
	 * <span class="street-address"> Hauptstrasse 158 </span> , <span
	 * class="postal-code">5742</span> <span class="locality">Kölliken</span>
	 * </p>
	 * </div> <div style="clear: both;"></div> </div>
	 * 
	 * 20101217js Bsp: (valid after 2010-12-13) * Now, every entry apparently
	 * seems to consist of: line1: <div data-slot="tel" class="searchResult..."
	 * line2: empty line3: <li class="detail">
	 * ... line4: empty
	 * 
	 * Please note (!) that the <li class="detail">tag is a part of the ADR_LIST
	 * display type now, which could be confusing to other people reviewing this
	 * code... And please note that one entry does NOT begin with the <li class "detail">
	 * tag, but (quite probably, I've not perfectly reviewed it) with the <div
	 * data-slot="... tag.
	 * 
	 * Please also note that the address/contact details seem to be included in
	 * either of the data carrying lines.
	 * 
	 * You might use the program tidy to re-format a downloaded result page to a
	 * more easily human readable style...
	 * 
	 * <div data-slot="tel" class="searchResult phonebookSearchResult vcard
	 * mappable ga ga\~entryBody\~entryClick yellowSearchResult
	 * "><div class="poiContainer"></div>< div
	 * class="poiImageContainer hidden"></div><span class="category"
	 * title="Treuhandgesellschaft Treuhandb&#xFC;ro; Buchhaltungsb&#xFC;ro"><a
	 * href="/de/q/bern/Treuhan dgesellschaft Treuhandb&#xFC;ro.html"
	 * title="Suche nach Treuhandgesellschaft Treuhandb&#xFC;ro in
	 * bern">Treuhandgesellschaft Treuhandb&#xFC;ro</a><span>; </span><a
	 * href="/de/q/bern/Buchhaltungsb&#xFC;ro.html"
	 * title="Suche nach Buchhaltungsb&#xFC;ro in bern"
	 * >Buchhaltungsb&#xFC;ro</a></span>
	 * <h3><a class="fn" href="http://tel.l
	 * ocal.ch/de/d/Bern/3008/Treuhandgesellschaft-Treuhandbuero
	 * /AAA-services-meier-franzelli-D6PmwphJZ6_tq_InnYTDIw
	 * ?what=meier&amp;where=bern">AAA services <span class="
	 * highlight">meier</span> + franzelli</a><br />
	 * </h3>
	 * <p class="contact phoneContact">
	 * <span class="label">Tel.: </span><span class="noads">* </span><span
	 * class="tel">< a class="phonenr" href="callto://+41313825082">031 382 50
	 * 82</a></span>
	 * </p>
	 * <p class="address adr">
	 * <span class="street-address">G&#xFC;terstrasse 22</span>, <span c
	 * lass="postal-code">3008</span> <span class="locality"><span
	 * class="highlight">Bern</span></span>
	 * </p>
	 * <ul class="links">
	 * 
	 * 
	 * <li class="detail"><img src=
	 * "http://s.staticlocal.ch/2/74023/s/resultlist/images/localinfo/detail.png"
	 * alt="" /><a class="detail ga ga\~entryMoreDetails\~e
	 * ntryClickResult" href="http://tel.local.ch/de/d/Bern/
	 * 3008/Treuhandgesellschaft-Treuhandbuero/AAA
	 * -services-meier-franzelli-D6PmwphJZ6_tq_InnYTDIw?what=meier&amp;whe
	 * re=bern">Mehr Details anzeigen</a></li>
	 * </ul>
	 * <div class="mapMarker"><span class="id">resultentry_0</span><span
	 * class="long">7.416991</span><span class="lat">46.9473 01</span><span
	 * class="head">AAA services <span class="highlight">meier</span> +
	 * franzelli</span><div class="body"><a class="fn"
	 * href="http://tel.local.ch/de/d/Bern
	 * /3008/Treuhandgesellschaft-Treuhandbuero/AAA-services
	 * -meier-franzelli-D6PmwphJZ6_tq_InnYTDIw?what
	 * =meier&amp;where=bern&amp;flyout=true">AAA services <span class="h
	 * ighlight">meier</span> + franzelli</a>
	 * <p class="address adr">
	 * <span class="street-address">G&#xFC;terstrasse 22</span>, <span
	 * class="postal-code">3008</span> <span class="locality"><span
	 * class="highlight">Bern</span></span>
	 * </p>
	 * <p class="contact phoneContact">
	 * <span class="label">Tel.: </span><span class="noads">* </span><span
	 * class="tel">031 382 50 82</span>
	 * </p>
	 * </div><span class="iconKey">tel/search</span></div></div>
	 * 
	 */
	private KontaktEntry extractListKontakt() throws IOException, MalformedURLException {

		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt() running...\n");
		System.out.print("jsdebug: Beginning of substrate: <"+extract("<",">")+"...\n");

		if (!moveTo(ADR_LISTENTRY_TAG)) { // Kein neuer Eintrag
			return null;
		}

		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt() extracting next entry...\n");

		// Name, Vorname, Zusatz

		// 20101213js, 20120712js
		// please note: the same marker used here should be re-used further
		// below.
		//
		// (Before 20101213js,) Original code was:
		// moveTo("<div class=\"entrybox\">");
		//
		// I tried these alternative markers, which didn't work as desired:
		// moveTo("<div class=\"detail\">");
		// moveTo("<li class=\"detail\">");
		// moveTo("<span class=\"id\">resultentry");
		// moveTo("<div class=\"body\">");
		//
		// (As of 20101213,) this one finally worked:
		// New code:
		// moveTo("<div class=\"poiContainer\">"); //20120712pre js

		//AS LONG AS we're using ADR_LISTENTRY_TAG to locate the first one (above)  //20120712js
		//we do NOT need to move on to exactly the same thing						//20120712js
		//to get to the start of the actual entry. So disabled the following line:  //20120712js
		// Please don't put a closing ">" after 'listing'; there is id=\...  ! 		//20120712js
		//moveTo(ADR_LISTENTRY_TAG); 												//20120712js

		// 20101213js
		// please note: the same marker used here has been used further above.
		//
		// Original code was:
		// int nextEntryPoxIndex = getNextPos("<div class=\"entrybox\">");
		//
		// New code:
		// int nextEntryPoxIndex = getNextPos("<div class=\"poiContainer\">");
		// //20120712pre js

		// Please don't put a closing ">" after 'listing'; there is id=\...  ! 		//20120712js
		//int nextEntryPoxIndex = getNextPos("<div class=\"listing\""); 			//20120712js
		int nextEntryPoxIndex = getNextPos(ADR_LISTENTRY_TAG); 					//20120712js
		
		//!!!!! 20120712js: The following debug code shows that we should indeed
		//!!!!!             use SINGLE quotation marks to search the next class entry,
		//!!!!!				EVEN if the file saved by mozilla shows double quotation marks.
		//!!!!! System.out.print("jsdebug: What kind of quotation marks are needed? extract(): "+extract("<div",">"));
		
		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt() nextEntryPoxIndex: " + nextEntryPoxIndex + "\n");

		// 20101213js-20101217js:
		// The attempted processing of Zusatz was somewhat disappointing.
		// Even before the change in tel.local.ch output format, I've seen
		// garbage including HTML fragments in the Zusatz field, which had to
		// be deleted by the user before the address could be stored.
		//
		// NOTA BENE: Beim Doppelklick auf einen Kontakt der Ergebnisliste wird
		// dieser offenbar nochmals neu abgefragt, und die *daraus erst*
		// resultierende
		// Detail-Information wird in die Elexis-internen Kontaktdaten
		// übernommen!!!
		// Deshalb ist es nicht ausreichend, die Zusatz-Abfrage hier
		// in extractListKontakt() zu entfernen, sondern insbesondere
		// bei extractKontakt() muss sie reviewt werden!
		//
		// Nota bene 2: I learned later that most probably, the problem comes
		// when some poBox field content is copied into the zusatz field,
		// if that had been empty before.
		// But if the 4 lines after String zusatz=""; are left active,
		// then e.g. Anne Müller receives the preceeding
		// "Suche nach Craniotherapy in Bern"
		// (including HTML code) in zusatz; if they are commented out; then
		// zusatz remains empty.
		// ...
		// Hmm. Reviewing the HTML source again...
		// OK: The problem is that tel.local.ch now delivers one "role" before
		// "fn",
		// which is a link to (I guess) searching all entries with the same role
		// in the same
		// locality;
		// and *another* "role" after "fn", which is the actual role field for
		// the current entry.
		//
		// Possibly, the "role" before "fn" could be now what "category" has
		// been
		// in a previous version of tel.local.ch output? Anyway, there's no
		// category field in
		// the exemplary output obtained by searching for Anne Müller in Bern
		// (who provides
		// Craniosacral Therapy)
		//
		// Example (outdated, as of 20101213 - but functionally similar (or
		// worse) as of 20120712js):
		//
		// </script></div><div id="results"><div id="singleview"
		// class="vcard"><div
		// class="resrowclr_yellow"><br /><img class="imgbox"
		// src="http://s.staticlocal.ch/images/pois/na/blue.png" alt="poi" /><p
		// class="role"><a
		// href="http://tel.local.ch/de/q/bern/Craniosacral Therapie.html"
		// title="Suche nach Craniosacral Therapie in bern">Craniosacral
		// Therapie</a>; <a
		// href="http://tel.local.ch/de/q/bern/Massage Gesundheits- und Sport-.html"
		// title="Suche nach Massage Gesundheits- und Sport- in bern">Massage
		// Gesundheits- und
		// Sport-</a>; <a
		// href="http://tel.local.ch/de/q/bern/Gesundheitspraxis.html"
		// title="Suche nach Gesundheitspraxis in bern">Gesundheitspraxis</a></p><h2
		// class="fn">Anne
		// M&#xFC;ller</h2><p class="role">dipl. Physiotherapeutin Mitglied
		// Cranio Suisse</p>
		//
		// N.B.: Anne Müller / Craniotherapy shows a role only in her
		// detail_format output, but not
		// if she appears within a list_format output.
		// If I double click on the first search result in the
		// medshare-directories form,
		// then a detail entry is processed (I can see this in my debug output)
		// -
		// where her title also becomes visible and known to the program -
		// BUT only the results from the list entry processing are transferred
		// into the following
		// dialog.
		// I can see that definitely if I hard-code some variable content in the
		// extractKontakt()
		// function,
		// or in the extractListKontakt() function.
		//
		// Hmmm. Only if I search for "anne müller craniotherapie" in "bern",
		// or for "anne müller" in "monbijoustrasse 12 bern",
		// the detail entry is processed and her title finally appears...
		//
		// But why's that the case?!
		//
		// I guess that anne müller is a very special case:
		// There are many anne müller search hits,
		// and probably, elexis only uses (all the) name information in order to
		// get the detailed
		// search entry to be processed into the following dialog. So in Anne
		// Müller's case,
		// ALL the name information is NOT sufficient to separate list entry 1
		// from all the other
		// entries, so even the refined search does NOT return a detail_format
		// result, but
		// returns a list again - without her title, of course.
		//
		// So we should probably use the name and the street info if available
		// to feed the request for details, not just the name.
		//
		//
		// Besides, the relevant entry is in a paragraph rather than a span tag
		// now:
		// <p class="role">THE ROLE</p>
		// instead of:
		// <span class="category">THE CATEGORY</span>
		//
		// So I disable looking for a zusatz in "category" here,
		// but rather look for zusatz in "role" after "fn" below:
		//
		// Old code to fill the zusatz field from "category":
		// int catIndex = getNextPos("<span class=\"category\"");
		// String zusatz = "";
		// if (catIndex > 0 && catIndex < nextEntryPoxIndex) {
		// moveTo("<span class=\"category\"");
		// zusatz = extract("\">", "</span>");
		// }

		// As of 20120712, the role field does (apparently) not exist any more
		// in list output,
		// and as I'm looking for profession (which probably replaced role) in
		// details output,
		// and category would be difficult to evaluate and suboptimal for actual
		// use,
		// I parse the list, and reformat it into a comma separated one. At
		// least for now.
		// Added a ToDo remark, though.

		// Name, Vorname

		// moveTo("<a class=\"fn\""); //20120712pre js
		//String nameVornameText = extract("\">", "</a>"); //$NON-NLS-1$ //$NON-NLS-2$	//20120712pre js

		moveTo("<h4><a href=\"http://tel.local.ch/"); // 20120712js
		String nameVornameText = extract("\">", "</a>"); //$NON-NLS-1$ //$NON-NLS-2$	//20120712js

		nameVornameText = removeDirt(nameVornameText);

		if (nameVornameText == null || nameVornameText.length() == 0) { // Keine
																		// leeren
																		// Inhalte
			return null;
		}
		String[] vornameNachname = getVornameNachname(nameVornameText);
		String vorname = vornameNachname[0];
		String nachname = vornameNachname[1];

		// Anne Müller case debug output:
		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt() nameVornameText: " + nameVornameText + "\n");

		// 20101217js
		// Also see comment related to "category" above.
		// New code to fill the zusatz field from "role" after "fn":

		// int catIndex = getNextPos("<p class=\"role\">"); //20120712pre js
		// String zusatz = ""; //20120712pre js
		// if (catIndex > 0 && catIndex < nextEntryPoxIndex) { //20120712pre js
		// moveTo("<p class=\"role\">"); //20120712pre js
		// zusatz = extractTo("</p>"); //20120712pre js
		// } //20120712pre js

		// 20120712js
		// Anstelle der "role" für eine Einzelperson kommt in der Liste offenbar
		// nur noch
		// eine Zeile, welche (eine oder auch mehrere) Kategorien aufzählt, zu
		// der diese Person gehört.
		// Z.B. für Jürg Hamacher, Bern:
		// <div class="categories">
		// <div class="dge"> </div>
		// <ul>
		// <li>Ärzte</li> (mit anderem Sonderzeichen für Ä)
		// <li>Innere Medizin</li>
		// <li>Lungenkrankheiten (Pneumologie)</li>
		// </ul>
		// Der Facharzt-Eintrag erscheint erst im Einzeleintrag=Details. :-(
		// Ich werte die Liste halt mal aus und ersetze sie durch eine
		// Komma-Getrennte Liste.
		//
		// To test this feature: Search for "hamacher" in "bern", this will return a list of 2 entries.
		// Double click on the first entry, to see the synthesized content of the "Zusatz" field.
		//
		// N.B.: If you directly search for "jürg hamacher" in "bern", this will return only ONE result,
		// which will be served in "Details" format, not "List" format. Therefore, the true "profession"
		// and "Title" fields are available; they are better candidates for the "Zusatz" field;
		// but the Details format is processed somewhere else in this java code file.
		//
		// N.B.: If you search for "meier" in "bern", this will render a larger list.
		// Interestingly, Meier Angela does NOT have category entries, and she is last on the first page -
		// so when catIndex is computed, it finds another <div class='categories'> from much further
		// below the interesting result table - and the evaluation jumps there, thereby skipping all
		// remaining content for Meier Angela fields, and returning some unwanted stuff in her Zusatz
		// field instead. -> Ergo: We REALLY MUST STRIP not only the footer, but actually everything
		// that comes after the list data and (at max) selection of additional list pages
		// just to make sure this problem does not happen.

		System.out
				.print("jsToDo:  DirectoriesContentParser.java: extractListKontakt() Possibly add better processing of a successor to role/categories/profession fields here as well, see comments above.\n");

		String zusatz = ""; // 20120712js
		int catIndex = getNextPos("<div class='categories'>"); // 20120712js
		if (catIndex > 0 && ((catIndex < nextEntryPoxIndex) || nextEntryPoxIndex==-1)) { 	// 20120712js
			moveTo("<div class='categories'>"); 				// 20120712js
			moveTo("<ul>"); // 20120712js
			zusatz = extractTo("</ul>"); 						// 20120712js
			zusatz = zusatz.replaceAll("(?sd)</li>\\s*<li>", ", "); 	// 20120712js
			zusatz = zusatz.replaceAll("<li>", ""); 					// 20120712js
			zusatz = zusatz.replaceAll("</li>", ""); 					// 20120712js
			zusatz = zusatz.replaceAll("^\n", ""); 						// 20120712js
			zusatz = zusatz.replaceAll("\n$", ""); 						// 20120712js
		} // 20120712js

		// Anne Müller case debug output:
		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt() catIndex: " + catIndex + "\n");
		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt() zusatz: \"" + zusatz + "\"\n\n");

		// As of 20120712js, Adresse comes first, and Phone etc. later;
		// so exchanged the sequence of the respective processing code sections.

		// Adresse, Ort, Plz

		// 201013js
		// Old code:
		// String adressTxt = extract("<p class=\"adr\">", "</p>");
		//
		// New code:
		// String adressTxt = extract("<p class=\"address adr\">", "</p>");
		// //20120712pre js
		String adressTxt = extract("<p class='address'>", "</p>"); // 20120712js

		System.out.print("jsdebug: DirectoriesContentParser.java: extractListKontakt().addressTxt:\n" + adressTxt + "\n\n");

		// 5.5.09 ts: verschachtelte spans -> alles bis zur nächsten span klasse
		// holen
		// 20101213js: I'm unaware why this should be needed here.
		// At the moment, it appears to be sufficient to get into strasse
		// everything till the next </span> tag, and the same for plz and ort.
		// I change it to be that way. If you are in doubt, you may want
		// to review if this really works for many search examples.
		//
		// What I observed multiple times is that a trailing extra space
		// was returned in the strasse field. I guess it will be best
		// to add its removal in the "removeDirt" function...
		//
		// Old code:
		// String strasse =
		// removeDirt(new
		// HtmlParser(adressTxt).extract("<span class=\"street-address\">",
		// ", <span class="));
		//
		// New code:
		// String strasse = //20120712pre js
		// removeDirt(new
		// HtmlParser(adressTxt).extract("<span class=\"street-address\">",
		// "</span>")); //20120712pre js
		// String plz = //20120712pre js
		// removeDirt(new
		// HtmlParser(adressTxt).extract("<span class=\"postal-code\">",
		// "</span>")); //20120712pre js
		// String ort = //20120712pre js
		// removeDirt(new
		// HtmlParser(adressTxt).extract("<span class=\"locality\">",
		// "</span>")); //20120712pre js

		// As of 20120712js, the format of adressTxt is now simply like:
		// "Musterstrasse 12, 3047 Bremgarten b. Bern"				(test case: search for: hamacher, bern)
		// "Gemeinschaftspraxis, Monbijoustrasse 124, 3007 Bern"	(test case: search for: anne müller, bern)
		String strasse = "";
		String plz = "";
		String ort = "";
		if (adressTxt.contains(", ")) {
			//Use lastIndexOf() to separate only PLZ Ort, no matter how many comma separted entries precede it.
			int CommaPos = adressTxt.lastIndexOf(", ");
			if (CommaPos > -1) {
				strasse = removeDirt(adressTxt.substring(0, CommaPos));
				int SpacePos = adressTxt.indexOf(" ", CommaPos + 2);
				if (SpacePos > -1) {
					plz = removeDirt(adressTxt.substring(CommaPos + 2, SpacePos));
					ort = removeDirt(adressTxt.substring(SpacePos + 1));
				} else {
					ort = removeDirt(adressTxt.substring(CommaPos + 2));
				}
			} else {
				ort = removeDirt(adressTxt);
			}
		}
		//20120712js: 
		//Now, if strasse contains a comma-space, and zusatz is still empty:
		//Then put the first part from strasse into zusatz.
		//You can test this feature by searching for "anne müller", "bern".
		//The "Gemeinschaftspraxis" will not appear in the list output (which has no field for zusatz),
		//but if you double click on the first entry (with Monbijoustrasse 124), you can see that
		//"Gemeinschaftspraxis" really was moved to the field "zusatz". This would not have happened
		//without the following lines (instead, it would remain preceeding strasse, separated by a comma).
		//Hmmm... later on: Well, as I've added the interpretation of categories unordered list into
		//the zusatz field, it is probably not empty right now any more.
		//So Gemeinschaftspraxis will stay below here.
		//OK. I then combine the content from this field before the comma,
		//and what might have been in zusatz already and put it there:
		if (strasse != "") {
			int CommaPos = strasse.lastIndexOf(", ");
			if (CommaPos > -1) {
				if (zusatz == "") { 
					zusatz = removeDirt(strasse.substring(0,CommaPos));
				}
				else {
					//Puts the new one to the end of the old one:
					//zusatz = zusatz.concat(" - "+removeDirt(strasse.substring(0,CommaPos)));
					//Puts the new one to the beginning of the old one:
					zusatz = removeDirt(strasse.substring(0,CommaPos))+" - "+zusatz;
				}
				strasse = removeDirt(strasse.substring(CommaPos+2));
			}
		}

		// Tel-Nr
		// moveTo("<span class=\"tel\""); //20120712pre js
		// moveTo("<a class=\"phonenr\""); //20120712pre js
		// String telNr = extract(">", "</a>").replace("&nbsp;",
		// "").replace("*", "").trim(); //20120712pre js

		// 20120712js   We want to parse the phone number also for the last entry in the list,
		//				where nextEntryPoxIndex will already be -1 (!).
		//				You can test that with meier, bern, or hamacher, bern.
		String telNr = ""; // 20120712js
		int phonePos=(getNextPos("<span class='phone'"));	// 20120712js
		if (phonePos>=0 && ((phonePos < nextEntryPoxIndex) || nextEntryPoxIndex==-1)) { // 20120712js
			moveTo("<span class='phone'"); 							// 20120712js
			moveTo("<label>Telefon"); 								// 20120712js
			// 20120713js Don't use "refuse number" but only "number" - the "refuse " is probably only there
			// for people who don't want to get called for advertising or a similar thing; there is a matching
			// note on the individual Details entries; and probably an asterisk displayed left of the phone number
			// in the List format output.
			moveTo("number\""); 									// 20120712js 
			telNr = extract(">", "</").replace("&nbsp;", "").replace("*", "").trim(); // 20120712js
		} // 20120712js

		
		//20120713js: Please note: Fax and E-mail are NOT available in the List format result

		return new KontaktEntry(vorname, nachname, zusatz, //$NON-NLS-1$
				strasse, plz, ort, telNr, "", "", false); //$NON-NLS-1$
	}
	
	/**
	  * Decodes the passed UTF-8 String using an algorithm that's compatible with
	  * JavaScript's <code>decodeURIComponent</code> function. Returns
	  * <code>null</code> if the String is <code>null</code>.
	  * 
	  * From: Utility class for JavaScript compatible UTF-8 encoding and decoding.
	  * 
	  * @see http://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-output
	  * @author John Topley 
	  *
	  * @param s The UTF-8 encoded String to be decoded
	  * @return the decoded String
	  */
	 public static String decodeURIComponent(String s)
	 {
	   if (s == null)
	   {
	     return null;
	   }
	   String result = null;
	   try
       {
         result = URLDecoder.decode(s, "UTF-8");
       }
	   // This exception should never occur.
       catch (UnsupportedEncodingException e)
       {
         result = s;  
       }
	   return result;
     }


	/**
	 * Extrahiert einen Kontakt aus einem Detaileintrag
	 * 
	 * Comment added to support testing 201012130311js: A result of this type
	 * should be obtainable by searching for: Wer, Was: eggimann meier Wo: bern
	 * 
	 * 
	 * 
	 * Bsp: (outdated, as of 20101213; newer version see below):
	 * 
	 * <div class="resrowclr_yellow"> </br> <img class="imgbox"
	 * src="http://s.staticlocal.ch/images/pois/na/blue.png" alt="poi"/>
	 * <p * class="role">
	 * Garage
	 * </p>
	 * <h2 class="fn">Auto Meier AG</h2>
	 * <p class="role">
	 * Opel-Vertretung
	 * </p>
	 * <div class="addressBlockMain"> <div class="streetAddress"> <span
	 * class="street-address">Hauptstrasse 253</span> </br> <span
	 * class="post-office-box">Postfach<br>
	 * </span> <span class="postal-code">5314</span> <span
	 * class="locality">Kleindöttingen</span> </div> </br>
	 * <table>
	 * <tbody>
	 * <tr class="phoneNumber">
	 * <td>
	 * <span class="contact">Telefon:</span></td>
	 * <td class="tel">
	 * <a class="phonenr" href="callto://+41562451818"> 056 245 18 18 </a></td>
	 * <td id="freecall"></td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * </div> <br class="bighr"/>
	 * <div id="moreAddresses"> <h3>Zusatzeintrag</h3> <div
	 * class="additionalAddress" id="additionalAddress1"> <span
	 * class="role">Verkauf</span> </br>
	 * <table>
	 * <tbody>
	 * <tr class="phoneNumber">
	 * <td>
	 * <span class="contact">Telefon:</span></td>
	 * <td class="tel">
	 * <a class="phonenr" href="callto://+41448104211"> 044 810 42 11 </a></td>
	 * <td id="freecall"></td>
	 * </tr>
	 * <tr>
	 * <td>
	 * <span class="contact">Fax:</span></td>
	 * <td>
	 * &nbsp;044 810 54 40</td>
	 * </tr>
	 * <tr>
	 * <td>&nbsp;</td>
	 * <td>&nbsp;</td>
	 * <td></td>
	 * </tr>
	 * <tr class="">
	 * <td>
	 * <span class="contact">E-Mail:</span></td>
	 * <td>
	 * &nbsp;<a href="mailto:info@kvd.ch"> info@kvd.ch </a></td>
	 * <td></td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * </div> </div> </div>
	 * 
	 * 
	 * 
	 * Bsp: (as of 20120712):
	 * 
	 * <div class="details" id="listing_wFbjkl2EHeP17FFOIF-Nnw"> <div style=
	 * "background: url(&quot;/assets/markers/sprite.png?1&quot;) no-repeat scroll -8px 0px transparent; width: 25px; height: 37px;"
	 * class="marker" data-icon="location" data-lat="46.960659"
	 * data-long="7.457951"></div> <div class="vcard"> <div class="title"> <h1 class="n fn">
	 * Eggimann Meier Rita</h1> </div> <div class="profession"></div>
	 * <p class="adr">
	 * <span class="street-address">Wiesenstrasse 39</span><br>
	 * <span class="region"><span class="postal-code">3014</span> <span
	 * class="locality">Bern</span></span>
	 * </p>
	 * <div class="contact">
	 * <table class="main">
	 * <tbody>
	 * <tr class="phone">
	 * <th class="label">
	 * Telefon:</th>
	 * <td>
	 * <span class="value"> <span class="star">*</span><a
	 * href="tel:+41313316128" class="refuse number">031 331 61 28</a> </span></td>
	 * </tr>
	 * 
	 * </tbody>
	 * </table>
	 * <table class="additional">
	 * </table>
	 * <table class="urls">
	 * </table>
	 * </div> </div>
	 * 
	 */
	private KontaktEntry extractKontakt() {

		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakt() running...\n");
		System.out.print("jsdebug: Beginning of substrate: <"+extract("<",">")+"...\n");

		if (!moveTo(ADR_SINGLEDETAILENTRY_TAG)) { // Kein neuer Eintrag
			return null;
		}

		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakt() extracting next entry...\n");

		// 20120712js: Title: This field appears before fn; it is not being
		// processed so far.
		System.out
				.print("jsToDo:  DirectoriesContentParser.java: extractKontakt() Add processing of the class='title', class='urls', and optionally class='region'. \n");

		// Name, Vorname
		// String nameVornameText = extract("<h2 class=\"fn\">", "</h2>");	// //20120712pre js

		// Possibly, some preprocessing gone astray(?) changed this from <h2 class='fn'> to <h1 class='n fn'>	//20120712js
		// Check the html dump in debugging output to see what is actually being processed here.				//20120712js
		String nameVornameText = extract("<h1 class='n fn'>", "</h1>"); 										//20120712js

		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakt().nameVornameText: \"" + nameVornameText + "\"\n");

		if (nameVornameText == null || nameVornameText.length() == 0) { // Keine leeren Inhalte
			return null;
		}
		String[] vornameNachname = getVornameNachname(nameVornameText);
		String vorname = vornameNachname[0];
		String nachname = vornameNachname[1];

		// Anne Müller case debug output:
		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakt() nameVornameText: " + nameVornameText + "\n");

		// Zusatz
		//
		// Comment added 20101213js:
		// Please note, that if zusatz remains empty, then further below it will
		// receive
		// the content of the poBox field. This however, would return garbage
		// (i.e. some
		// remainders of PLZ and ORT plus HTML tag leftovers) in versions before
		// 2010-12-13.
		// That garbage, however, was introduced from the poBox related code
		// below,
		// where plzCode was filled even when the corresponding tag was not
		// available,
		// whereas the following 4 lines are (and have been) apparently ok:
		String zusatz = "";
		// if (moveTo("<p class=\"role\">")) { //20120712pre js
		// zusatz = extractTo("</p>"); //20120712pre js
		if (moveTo("<div class='profession'>")) { // 20120712js
			zusatz = extractTo("</div>"); // 20120712js
		}

		// Anne Müller case debug output:
		System.out.print("jsdebug: DirectoriesContentParser.java: extractKontakt() zusatz: \"" + zusatz + "\"\n\n");

		// Adresse (this is a two level record, both before and after
		// 20120712js):

		// String adressTxt = extract("<div class=\"streetAddress\">", "</div>"); //20120712pre js
		
		//Please note: We need single quotation marks around class='adr' (and other class tags),
		//but double quotation marks around class=\"street-address" (and others in the second level).
		//What mozilla firefox saves from downloads is NOT exactly what is processed here. 20120712js
		String adressTxt = extract("<p class='adr'>", "</p>"); // 20120712js
		
		// Anne Müller, Bern or Eggimann Meier, Bern case debug output:
		System.out.print("jsdebug: DirectoriesContentParser.java: adressTxt: " + adressTxt+"\n");

		HtmlParser parser = new HtmlParser(adressTxt);

		//String streetAddress = removeDirt(parser.extract("<span class=\"street-address\">", "</span>")); // 20120712js:
		String streetAddress = "";									// 20120712js		
		if (adressTxt.contains("<span class=\"street-address\">")) { // 20120712js
			streetAddress = removeDirt(parser.extract("<span class=\"street-address\">", "</span>")); // 20120712js
		}													// 20120712js
																											// unchanged
		// 20101213js:
		// The simple (unconditional):
		//
		// Old code:
		// String poBox =
		// removeDirt(parser.extract("<span class=\"post-office-box\">",
		// "</span>"));
		//
		// would not really work if there was no post-office-box available at
		// all.
		// In that case, it might return garbage.
		// And if zusatz was also empty (see further above), then any bad
		// content
		// of poBox would be propagated up to zusatz.
		// Therefore, we want to be a bit more careful with doing anything into
		// poBox:
		//
		// New code:
		//String poBox = "";								// 20120712pre js
		//if (moveTo("<span class=\"post-office-box\">")) { // 20120712pre js
		//	poBox = removeDirt(extractTo("</span>"));		// 20120712pre js
		//}													// 20120712pre js
		// Ja, so ist es gut :-)							// 20120712pre js
	
		// 20120713js revised:
		String poBox = "";									// 20120712js		
		if (adressTxt.contains("<span class=\"poBox\">")) { // 20120712js
			poBox = removeDirt(parser.extract("<span class=\"poBox\">", "</span>")); // 20120712js
		}													// 20120712js

		// plzCode
		//
		// 20101217js:
		// It's probably better to also fill plzCode ONLY when moveTo() would
		// not fail.
		//
		// Old code:
		// String plzCode =
		// removeDirt(parser.extract("<span class=\"postal-code\">",
		// "</span>"));
		//
		// New code:
		//String plzCode = "";							// 20120712pre js
		//if (moveTo("<span class=\"postal-code\">")) { // 20120712pre js			
		//	plzCode = removeDirt(extractTo("</span>"));	// 20120712pre js
		//}												// 20120712pre js

		// 20120713js revised:
		String plzCode = "";									// 20120712js
		if (adressTxt.contains("<span class=\"postal-code\">")) {	// 20120712js
			plzCode = removeDirt(parser.extract("<span class=\"postal-code\">", "</span>")); // 20120712js
		}													// 20120712js

		// 20120712js: Region:
		// There is an additional field class="region" available now in local.ch
		// output,
		// which follows the "street-address" field and precedes "locality".
		// But it's apparently not used in Elexis so far, and I don't add it
		// now.

		// Ort
		//
		// 20101217js:
		// It's probably better to also fill Ort ONLY when moveTo() would not
		// fail.
		//
		// Older code:
		// String ort = removeDirt(new
		// HtmlParser(adressTxt).extract("<span class=\"locality\">",
		// "</span>"));
		//
		// Old code:
		// parser.moveTo("<tr class=\"locality\">");
		// parser.moveTo("<a href=");
		// String ort = removeDirt(parser.extract(">", "</a>").replace("&nbsp;",
		// "").trim());
		//
		// New code:
		// String ort = "";
		// if (moveTo("<tr class=\"locality\">")) { //20120712pre js
		// moveTo("<a href="); //20120712pre js
		// ort = removeDirt(parser.extract(">", "</a>").replace("&nbsp;",
		// "").trim()); //20120712pre js
		// } //20120712pre js

		// 20120713js revised:
		String ort = "";									// 20120712js
		if (adressTxt.contains("<span class=\"locality\">")) {	// 20120712js
			ort = removeDirt(parser.extract("<span class=\"locality\">", "</span>")); // 20120712js
		}													// 20120712js

		// If zusatz is empty, then we copy the content of poBox into zusatz.
		if (zusatz == null || zusatz.length() == 0) {
			zusatz = poBox;
		}

		// Tel/Fax & Email

		// moveTo("<tr class=\"phoneNumber\">"); //20120712pre js
		// String tel = ""; //20120712pre js
		// if (moveTo("<span class=\"contact\">Telefon")) { //20120712pre js
		// moveTo("<td class=\"tel\""); //20120712pre js
		// moveTo("<a class=\"phonenr\""); //20120712pre js
		// tel = extract(">", "</a>").replace("&nbsp;", "").replace("*",
		// "").trim(); //20120712pre js
		// } //20120712pre js

		//Please note: some class tags need single, others need double quotation marks. // 20120712js
		String tel = ""; // 20120712js
		if (moveTo("<tr class='phone'>")) { // 20120712js
			if (moveTo("<th class='label'>\nTelefon:")) { // 20120712js
				// 20120713js Don't use "refuse number" but only "number" - the "refuse " is probably only there
				// for people who don't want to get called for advertising or a similar thing; there is a matching
				// note on the individual Details entries; and probably an asterisk displayed left of the phone number
				// in the List format output.
				moveTo("number\""); 									// 20120712js 
				tel = extract(">", "</").replace("&nbsp;", "").replace("*", "").trim(); // 20120712js
			}
		}

		// String fax = ""; //20120712pre js
		// if (moveTo("<span class=\"contact\">Fax")) { //20120712pre js
		// fax = extract("<td>", "</td>").replace("&nbsp;", "").replace("*",
		// "").trim(); //20120712pre js
		// } //20120712pre js

		String fax = ""; // 20120712js
		if (moveTo("<tr class='fax'>")) { // 20120712js
			if (moveTo("<th class='label'>\nFax:")) { // 20120712js
				// 20120713js Don't use "refuse number" but only "number" - the "refuse " is probably only there
				// for people who don't want to get called for advertising or a similar thing; there is a matching
				// note on the individual Details entries; and probably an asterisk displayed left of the phone number
				// in the List format output.
				moveTo("number\""); 									// 20120712js 
				fax = extract(">", "</").replace("&nbsp;", "").replace("*", "").trim(); // 20120712js
			} // 20120712js
		} // 20120712js

		// String email = ""; //20120712pre js
		// if (moveTo("<span class=\"contact\">E-Mail")) { //20120712pre js
		// moveTo("<span class=\"obfuscml\""); //20120712pre js
		// email = extract("\">", "</span>"); //20120712pre js
		// // Email Adresse wird verkehrt gesendet //20120712pre js
		// email = reverseString(email); //20120712pre js
		// } //20120712pre js

		String email = ""; // 20120712js
		if (moveTo("<tr class='email'")) { // 20120712js
			if (moveTo("<th class='label'>\nE-Mail:")) { // 20120712js
				//moveTo("<a href=\"mailto"); // 20120712js
				//e-mail is served in an URI encoded format - so decode this, therefore added a separate function above
				moveTo("decodeURIComponent(");// 20120712js	
				email = decodeURIComponent(extract("'", "')")); // 20120712js
				//the e-mail entry also contains javascript documentwrite('<a href='\"mailto:user@server.ch\">user@server.ch<\/a>'); wrapped around it.
				//remove all that is not needed...
				email = email.replaceFirst("^.*mailto:.*\\\">",""); // 20120712js 
				email = email.replaceFirst("<..a>'.;$","");
			} // 20120712js
		}

		return new KontaktEntry(vorname, nachname, zusatz, streetAddress, plzCode, ort, tel, fax, email, true);
	}
}
