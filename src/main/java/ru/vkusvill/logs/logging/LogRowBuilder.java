package ru.vkusvill.logs.logging;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class LogRowBuilder {

    private static final String NA_VALUE = "_N\\A_";

    public String extractStrPar(String formBody) {
        if (formBody == null || formBody.isEmpty()) return "";
        String[] parts = formBody.split("&");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("str_par=")) {
                String encoded = p.substring("str_par=".length());
                try {
                    // заменяем + на пробел и декодируем как UTF-8
                    return URLDecoder.decode(encoded.replace("+", "%20"), "UTF-8");
                } catch (Exception e) {
                    return encoded;
                }
            }
        }
        return formBody;
    }

    public String extractValueOrNA(String text, String marker) {
        if (text == null || text.isEmpty()) return NA_VALUE;
        int start = text.indexOf(marker);
        if (start == -1) return NA_VALUE;
        start += marker.length();
        int end = text.indexOf("]}", start);
        if (end == -1) return NA_VALUE;
        String val = text.substring(start, end).trim();
        if ("[]".equals(val) || val.isEmpty()) {
            return "";
        }
        return val;
    }

    public String extractRnTov(String strPar) {
        if (strPar == null || strPar.isEmpty()) return "";
        String marker = "{[rn_tov]}{[";
        int start = strPar.indexOf(marker);
        if (start == -1) return "";
        start += marker.length();
        int end = strPar.indexOf("]}", start);
        if (end == -1) return "";
        return strPar.substring(start, end).trim();
    }

    public String autoDetectCase(String strPar) {
        if (strPar == null || strPar.isEmpty()) return "";

        String rn = extractRnTov(strPar);
        String suffix = (rn != null && rn.length() > 0) ? rn + "-й" : "";

        // аналог has(pattern) из скрипта
        java.util.function.Predicate<String> has = new java.util.function.Predicate<String>() {
            @Override
            public boolean test(String pattern) {
                return strPar.indexOf(pattern) > -1;
            }
        };

        if (has.test("{[id_element]}{[start]}")) {
            return "Тап_на_поиск";
        }

        if (has.test("{[id_element]}{[not_found]}") && has.test("{[search_bar]}{[1111]}")) {
            return "Ввести_запрос_по_которому_не_будет_выдачи_1111";
        }

        if (has.test("{[id_element]}{[carousel]}") && has.test("{[widget_title]}{[Товары не в наличии]}")) {
            return "Скролл_до_товаров_Не_в_наличии";
        }

        if (has.test("{[id_element]}{[filter]}")) {
            return "Тап_на_блок_фильтров";
        }

        if (has.test("{[id_element]}{[search_words]}") && has.test("{[action]}{[tap]}")) {
            return "Тап_на_подсказку_при_вводе";
        }

        if (has.test("{[id_element]}{[search_links]}") &&
                has.test("{[action]}{[tap]}") &&
                has.test("{[widget_type]}{[category_tips]}")) {
            return "Переход_в_подсказки_классы";
        }

        if (has.test("{[button_name]}{[Доставить завтра]}")) {
            return "Тап_на_Доставить_завтра";
        }

        if (has.test("{[id_element]}{[product_detail]}")) {
            return suffix.length() == 0
                    ? "Открыть_карточку_товара"
                    : "Открыть_карточку_" + suffix + "_товара";
        }

        if (has.test("{[id_element]}{[add]}")) {
            return suffix.length() == 0
                    ? "Тап_В_корзину_на_товаре"
                    : "Тап_В_корзину_на_" + suffix + "_товаре";
        }

        if (has.test("{[id_element]}{[plus]}")) {
            return suffix.length() == 0
                    ? "Прибавить_количество_товара_+"
                    : "Прибавить_количество_" + suffix + "_товара_+";
        }

        if (has.test("{[id_element]}{[minus]}")) {
            return suffix.length() == 0
                    ? "Убавить_количество_товара_-"
                    : "Убавить_количество_" + suffix + "_товара_-";
        }

        if (has.test("{[id_element]}{[edit]}")) {
            return suffix.length() == 0
                    ? "Изменить_количество_вручную_у_товара"
                    : "Изменить_количество_вручную_у_" + suffix + "_товара";
        }

        if (has.test("{[id_element]}{[add_fav]}")) {
            return suffix.length() == 0
                    ? "Добавить_в_избранное_товар"
                    : "Добавить_в_избранное_" + suffix + "_товар";
        }

        if (has.test("{[id_element]}{[del_fav]}")) {
            return suffix.length() == 0
                    ? "Убрать_из_избранного_товар"
                    : "Убрать_из_избранного_" + suffix + "_товар";
        }

        if (has.test("{[id_element]}{[add_list]}")) {
            return suffix.length() == 0
                    ? "Нажать_на_кнопку_Добавить_в_список_у_товара"
                    : "Нажать_на_кнопку_Добавить_в_список_у_" + suffix + "_товара";
        }

        if (has.test("{[id_element]}{[back]}")) {
            return "Тап_назад";
        }

        return "";
    }

    public List<String> buildRow(String formBody, String caseNameFromRequest) {
        String strPar = extractStrPar(formBody);
        String number = extractValueOrNA(strPar, "{[number]}{[");
        String date_add = extractValueOrNA(strPar, "{[date_add]}{[");
        String autoCase = autoDetectCase(strPar);
        String caseName;

        if (caseNameFromRequest != null && caseNameFromRequest.trim().length() > 0) {
            caseName = caseNameFromRequest;
        } else {
            caseName = autoCase;
        }

        String id_order            = extractValueOrNA(strPar, "{[id_order]}{[");
        String category_id         = extractValueOrNA(strPar, "{[category_id]}{[");
        String button_name         = extractValueOrNA(strPar, "{[button_name]}{[");
        String id_screen           = extractValueOrNA(strPar, "{[id_screen]}{[");
        String id_element          = extractValueOrNA(strPar, "{[id_element]}{[");
        String actionField         = extractValueOrNA(strPar, "{[action]}{[");
        String formname            = extractValueOrNA(strPar, "{[formname]}{[");
        String search_bar          = extractValueOrNA(strPar, "{[search_bar]}{[");
        String search_bar_fixed    = extractValueOrNA(strPar, "{[search_bar_fixed]}{[");
        String tovs_subscribe      = extractValueOrNA(strPar, "{[tovs_subscribe_shown]}{[");
        String rn_tov              = extractValueOrNA(strPar, "{[rn_tov]}{[");
        String tovs_rn_shown_sort  = extractValueOrNA(strPar, "{[tovs_rn_shown_sort]}{[");
        String tovs_rn_disc_sort   = extractValueOrNA(strPar, "{[tovs_rn_shown_discount_sort]}{[");
        String falseteasers_sh     = extractValueOrNA(strPar, "{[falseteasers_shown]}{[");
        String id_tov              = extractValueOrNA(strPar, "{[id_tov]}{[");
        String tov_reason_real     = extractValueOrNA(strPar, "{[tov_reason_array_real]}{[");
        String tovs_rn_na_sort     = extractValueOrNA(strPar, "{[tovs_rn_na_sort]}{[");
        String tovs_rn_real_sort   = extractValueOrNA(strPar, "{[tovs_rn_real_sort]}{[");
        String rn_green            = extractValueOrNA(strPar, "{[rn_green]}{[");
        String rn_max              = extractValueOrNA(strPar, "{[rn_max]}{[");
        String tovs_rn_archive     = extractValueOrNA(strPar, "{[tovs_rn_archive]}{[");
        String name_tov            = extractValueOrNA(strPar, "{[name_tov]}{[");
        String tovs_rn_arch_sort   = extractValueOrNA(strPar, "{[tovs_rn_archive_sort]}{[");
        String widget_id           = extractValueOrNA(strPar, "{[widget_id]}{[");
        String widget_type         = extractValueOrNA(strPar, "{[widget_type]}{[");
        String widget_title        = extractValueOrNA(strPar, "{[widget_title]}{[");
        String group_list          = extractValueOrNA(strPar, "{[group_list]}{[");
        String boost_array         = extractValueOrNA(strPar, "{[boost_array]}{[");
        String id_tov_boost_arr    = extractValueOrNA(strPar, "{[id_tov_boost_array]}{[");
        String position_boost_arr  = extractValueOrNA(strPar, "{[position_boost_array]}{[");
        String id_split_array      = extractValueOrNA(strPar, "{[id_split_array]}{[");
        String boost_na_array      = extractValueOrNA(strPar, "{[boost_na_array]}{[");
        String id_tov_boost_na     = extractValueOrNA(strPar, "{[id_tov_boost_na_array]}{[");
        String position_boost_na   = extractValueOrNA(strPar, "{[position_boost_na_array]}{[");
        String id_cohort           = extractValueOrNA(strPar, "{[id_cohort]}{[");
        String id_split_na_array   = extractValueOrNA(strPar, "{[id_split_na_array]}{[");
        String tov_reason          = extractValueOrNA(strPar, "{[tov_reason]}{[");
        String tov_reason_na       = extractValueOrNA(strPar, "{[tov_reason_array_na]}{[");
        String ref_widget_id       = extractValueOrNA(strPar, "{[ref_widget_id]}{[");
        String section_name        = extractValueOrNA(strPar, "{[section_name]}{[");
        String shop_list           = extractValueOrNA(strPar, "{[shop_list]}{[");
        String shop_list_full      = extractValueOrNA(strPar, "{[shop_list_full]}{[");
        String rn_learn            = extractValueOrNA(strPar, "{[rn_learn]}{[");
        String pers_listing        = extractValueOrNA(strPar, "{[pers_listing]}{[");
        String pers_tov            = extractValueOrNA(strPar, "{[pers_tov]}{[");
        String id_tov_boost        = extractValueOrNA(strPar, "{[id_tov_boost]}{[");
        String id_boost            = extractValueOrNA(strPar, "{[id_boost]}{[");
        String boost_position      = extractValueOrNA(strPar, "{[boost_position]}{[");
        String product_detail      = extractValueOrNA(strPar, "{[product_detail]}{[");
        String iminshop            = extractValueOrNA(strPar, "{[iminshop]}{[");
        String chosenmark          = extractValueOrNA(strPar, "{[chosenmark]}{[");
        String specialmark         = extractValueOrNA(strPar, "{[specialmark]}{[");
        String lp_today            = extractValueOrNA(strPar, "{[lp_today]}{[");
        String greenmark = extractValueOrNA(strPar, "{[greenmark]}{[");
        String lp_tomorrow         = extractValueOrNA(strPar, "{[lp_tomorrow]}{[");
        String amount              = extractValueOrNA(strPar, "{[amount]}{[");
        String cart_qty            = extractValueOrNA(strPar, "{[cart_qty]}{[");
        String stock               = extractValueOrNA(strPar, "{[stock]}{[");

        List<String> row = new ArrayList<String>();
        row.add(caseName);          // A: Кейс
        row.add("str_par");         // B: Действие
        row.add(strPar);            // C: str_par

        row.add(number);
        row.add(date_add);
        row.add(id_order);
        row.add(category_id);
        row.add(button_name);
        row.add(id_screen);
        row.add(id_element);
        row.add(actionField);
        row.add(formname);
        row.add(search_bar);
        row.add(search_bar_fixed);
        row.add(tovs_subscribe);
        row.add(rn_tov);
        row.add(tovs_rn_shown_sort);
        row.add(tovs_rn_disc_sort);
        row.add(falseteasers_sh);
        row.add(id_tov);
        row.add(tov_reason_real);
        row.add(tovs_rn_na_sort);
        row.add(tovs_rn_real_sort);
        row.add(rn_green);
        row.add(rn_max);
        row.add(tovs_rn_archive);
        row.add(name_tov);
        row.add(tovs_rn_arch_sort);
        row.add(widget_id);
        row.add(widget_type);
        row.add(widget_title);
        row.add(group_list);
        row.add(boost_array);
        row.add(id_tov_boost_arr);
        row.add(position_boost_arr);
        row.add(id_split_array);
        row.add(boost_na_array);
        row.add(id_tov_boost_na);
        row.add(position_boost_na);
        row.add(id_cohort);
        row.add(id_split_na_array);
        row.add(tov_reason);
        row.add(tov_reason_na);
        row.add(ref_widget_id);
        row.add(section_name);
        row.add(shop_list);
        row.add(shop_list_full);
        row.add(rn_learn);
        row.add(pers_listing);
        row.add(pers_tov);
        row.add(id_tov_boost);
        row.add(id_boost);
        row.add(boost_position);
        row.add(product_detail);
        row.add(iminshop);
        row.add(chosenmark);
        row.add(specialmark);
        row.add(lp_today);
        row.add(lp_tomorrow);
        row.add(amount);
        row.add(cart_qty);
        row.add(stock);

        return row;
    }
}
