/*!
 * ng-obiba - v1.4.0
 * https://github.com/obiba/ng-obiba

 * License: GNU Public License version 3
 * Date: 2016-05-12
 */
'use strict';

angular.module('obiba.utils', [])

  .constant('ObibaCountriesIsoCodes', {
    'en': [
      {'code': 'AND', 'name': 'Andorra'},
      {'code': 'ARE', 'name': 'United Arab Emirates'},
      {'code': 'AFG', 'name': 'Afghanistan'},
      {'code': 'ATG', 'name': 'Antigua and Barbuda'},
      {'code': 'AIA', 'name': 'Anguilla'},
      {'code': 'ALB', 'name': 'Albania'},
      {'code': 'ARM', 'name': 'Armenia'},
      {'code': 'AGO', 'name': 'Angola'},
      {'code': 'ATA', 'name': 'Antarctica'},
      {'code': 'ARG', 'name': 'Argentina'},
      {'code': 'ASM', 'name': 'American Samoa'},
      {'code': 'AUT', 'name': 'Austria'},
      {'code': 'AUS', 'name': 'Australia'},
      {'code': 'ABW', 'name': 'Aruba'},
      {'code': 'ALA', 'name': 'Åland Islands'},
      {'code': 'AZE', 'name': 'Azerbaijan'},
      {'code': 'BIH', 'name': 'Bosnia and Herzegovina'},
      {'code': 'BRB', 'name': 'Barbados'},
      {'code': 'BGD', 'name': 'Bangladesh'},
      {'code': 'BEL', 'name': 'Belgium'},
      {'code': 'BFA', 'name': 'Burkina Faso'},
      {'code': 'BGR', 'name': 'Bulgaria'},
      {'code': 'BHR', 'name': 'Bahrain'},
      {'code': 'BDI', 'name': 'Burundi'},
      {'code': 'BEN', 'name': 'Benin'},
      {'code': 'BLM', 'name': 'Saint Barthélemy'},
      {'code': 'BMU', 'name': 'Bermuda'},
      {'code': 'BRN', 'name': 'Brunei Darussalam'},
      {'code': 'BOL', 'name': 'Bolivia, Plurinational State of'},
      {'code': 'BES', 'name': 'Bonaire, Sint Eustatius and Saba'},
      {'code': 'BRA', 'name': 'Brazil'},
      {'code': 'BHS', 'name': 'Bahamas'},
      {'code': 'BTN', 'name': 'Bhutan'},
      {'code': 'BVT', 'name': 'Bouvet Island'},
      {'code': 'BWA', 'name': 'Botswana'},
      {'code': 'BLR', 'name': 'Belarus'},
      {'code': 'BLZ', 'name': 'Belize'},
      {'code': 'CAN', 'name': 'Canada'},
      {'code': 'CCK', 'name': 'Cocos (Keeling) Islands'},
      {'code': 'COD', 'name': 'Congo, the Democratic Republic of the'},
      {'code': 'CAF', 'name': 'Central African Republic'},
      {'code': 'COG', 'name': 'Congo'},
      {'code': 'CHE', 'name': 'Switzerland'},
      {'code': 'CIV', 'name': 'Côte d\'Ivoire'},
      {'code': 'COK', 'name': 'Cook Islands'},
      {'code': 'CHL', 'name': 'Chile'},
      {'code': 'CMR', 'name': 'Cameroon'},
      {'code': 'CHN', 'name': 'China'},
      {'code': 'COL', 'name': 'Colombia'},
      {'code': 'CRI', 'name': 'Costa Rica'},
      {'code': 'CUB', 'name': 'Cuba'},
      {'code': 'CPV', 'name': 'Cabo Verde'},
      {'code': 'CUW', 'name': 'Curaçao'},
      {'code': 'CXR', 'name': 'Christmas Island'},
      {'code': 'CYP', 'name': 'Cyprus'},
      {'code': 'CZE', 'name': 'Czech Republic'},
      {'code': 'DEU', 'name': 'Germany'},
      {'code': 'DJI', 'name': 'Djibouti'},
      {'code': 'DNK', 'name': 'Denmark'},
      {'code': 'DMA', 'name': 'Dominica'},
      {'code': 'DOM', 'name': 'Dominican Republic'},
      {'code': 'DZA', 'name': 'Algeria'},
      {'code': 'ECU', 'name': 'Ecuador'},
      {'code': 'EST', 'name': 'Estonia'},
      {'code': 'EGY', 'name': 'Egypt'},
      {'code': 'ESH', 'name': 'Western Sahara'},
      {'code': 'ERI', 'name': 'Eritrea'},
      {'code': 'ESP', 'name': 'Spain'},
      {'code': 'ETH', 'name': 'Ethiopia'},
      {'code': 'FIN', 'name': 'Finland'},
      {'code': 'FJI', 'name': 'Fiji'},
      {'code': 'FLK', 'name': 'Falkland Islands (Malvinas)'},
      {'code': 'FSM', 'name': 'Micronesia, Federated States of'},
      {'code': 'FRO', 'name': 'Faroe Islands'},
      {'code': 'FRA', 'name': 'France'},
      {'code': 'GAB', 'name': 'Gabon'},
      {'code': 'GBR', 'name': 'United Kingdom of Great Britain and Northern Ir'},
      {'code': 'GRD', 'name': 'Grenada'},
      {'code': 'GEO', 'name': 'Georgia'},
      {'code': 'GUF', 'name': 'French Guiana'},
      {'code': 'GGY', 'name': 'Guernsey'},
      {'code': 'GHA', 'name': 'Ghana'},
      {'code': 'GIB', 'name': 'Gibraltar'},
      {'code': 'GRL', 'name': 'Greenland'},
      {'code': 'GMB', 'name': 'Gambia'},
      {'code': 'GIN', 'name': 'Guinea'},
      {'code': 'GLP', 'name': 'Guadeloupe'},
      {'code': 'GNQ', 'name': 'Equatorial Guinea'},
      {'code': 'GRC', 'name': 'Greece'},
      {'code': 'SGS', 'name': 'South Georgia and the South Sandwich Islands'},
      {'code': 'GTM', 'name': 'Guatemala'},
      {'code': 'GUM', 'name': 'Guam'},
      {'code': 'GNB', 'name': 'Guinea-Bissau'},
      {'code': 'GUY', 'name': 'Guyana'},
      {'code': 'HKG', 'name': 'Hong Kong'},
      {'code': 'HMD', 'name': 'Heard Island and McDonald Islands'},
      {'code': 'HND', 'name': 'Honduras'},
      {'code': 'HRV', 'name': 'Croatia'},
      {'code': 'HTI', 'name': 'Haiti'},
      {'code': 'HUN', 'name': 'Hungary'},
      {'code': 'IDN', 'name': 'Indonesia'},
      {'code': 'IRL', 'name': 'Ireland'},
      {'code': 'ISR', 'name': 'Israel'},
      {'code': 'IMN', 'name': 'Isle of Man'},
      {'code': 'IND', 'name': 'India'},
      {'code': 'IOT', 'name': 'British Indian Ocean Territory'},
      {'code': 'IRQ', 'name': 'Iraq'},
      {'code': 'IRN', 'name': 'Iran, Islamic Republic of'},
      {'code': 'ISL', 'name': 'Iceland'},
      {'code': 'ITA', 'name': 'Italy'},
      {'code': 'JEY', 'name': 'Jersey'},
      {'code': 'JAM', 'name': 'Jamaica'},
      {'code': 'JOR', 'name': 'Jordan'},
      {'code': 'JPN', 'name': 'Japan'},
      {'code': 'KEN', 'name': 'Kenya'},
      {'code': 'KGZ', 'name': 'Kyrgyzstan'},
      {'code': 'KHM', 'name': 'Cambodia'},
      {'code': 'KIR', 'name': 'Kiribati'},
      {'code': 'COM', 'name': 'Comoros'},
      {'code': 'KNA', 'name': 'Saint Kitts and Nevis'},
      {'code': 'PRK', 'name': 'Korea, Democratic People\'s Republic of'},
      {'code': 'KOR', 'name': 'Korea, Republic of'},
      {'code': 'KWT', 'name': 'Kuwait'},
      {'code': 'CYM', 'name': 'Cayman Islands'},
      {'code': 'KAZ', 'name': 'Kazakhstan'},
      {'code': 'LAO', 'name': 'Lao People\'s Democratic Republic'},
      {'code': 'LBN', 'name': 'Lebanon'},
      {'code': 'LCA', 'name': 'Saint Lucia'},
      {'code': 'LIE', 'name': 'Liechtenstein'},
      {'code': 'LKA', 'name': 'Sri Lanka'},
      {'code': 'LBR', 'name': 'Liberia'},
      {'code': 'LSO', 'name': 'Lesotho'},
      {'code': 'LTU', 'name': 'Lithuania'},
      {'code': 'LUX', 'name': 'Luxembourg'},
      {'code': 'LVA', 'name': 'Latvia'},
      {'code': 'LBY', 'name': 'Libya'},
      {'code': 'MAR', 'name': 'Morocco'},
      {'code': 'MCO', 'name': 'Monaco'},
      {'code': 'MDA', 'name': 'Moldova, Republic of'},
      {'code': 'MNE', 'name': 'Montenegro'},
      {'code': 'MAF', 'name': 'Saint Martin (French part)'},
      {'code': 'MDG', 'name': 'Madagascar'},
      {'code': 'MHL', 'name': 'Marshall Islands'},
      {'code': 'MKD', 'name': 'Macedonia, the former Yugoslav Republic of'},
      {'code': 'MLI', 'name': 'Mali'},
      {'code': 'MMR', 'name': 'Myanmar'},
      {'code': 'MNG', 'name': 'Mongolia'},
      {'code': 'MAC', 'name': 'Macao'},
      {'code': 'MNP', 'name': 'Northern Mariana Islands'},
      {'code': 'MTQ', 'name': 'Martinique'},
      {'code': 'MRT', 'name': 'Mauritania'},
      {'code': 'MSR', 'name': 'Montserrat'},
      {'code': 'MLT', 'name': 'Malta'},
      {'code': 'MUS', 'name': 'Mauritius'},
      {'code': 'MDV', 'name': 'Maldives'},
      {'code': 'MWI', 'name': 'Malawi'},
      {'code': 'MEX', 'name': 'Mexico'},
      {'code': 'MYS', 'name': 'Malaysia'},
      {'code': 'MOZ', 'name': 'Mozambique'},
      {'code': 'NAM', 'name': 'Namibia'},
      {'code': 'NCL', 'name': 'New Caledonia'},
      {'code': 'NER', 'name': 'Niger'},
      {'code': 'NFK', 'name': 'Norfolk Island'},
      {'code': 'NGA', 'name': 'Nigeria'},
      {'code': 'NIC', 'name': 'Nicaragua'},
      {'code': 'NLD', 'name': 'Netherlands'},
      {'code': 'NOR', 'name': 'Norway'},
      {'code': 'NPL', 'name': 'Nepal'},
      {'code': 'NRU', 'name': 'Nauru'},
      {'code': 'NIU', 'name': 'Niue'},
      {'code': 'NZL', 'name': 'New Zealand'},
      {'code': 'OMN', 'name': 'Oman'},
      {'code': 'PAN', 'name': 'Panama'},
      {'code': 'PER', 'name': 'Peru'},
      {'code': 'PYF', 'name': 'French Polynesia'},
      {'code': 'PNG', 'name': 'Papua New Guinea'},
      {'code': 'PHL', 'name': 'Philippines'},
      {'code': 'PAK', 'name': 'Pakistan'},
      {'code': 'POL', 'name': 'Poland'},
      {'code': 'SPM', 'name': 'Saint Pierre and Miquelon'},
      {'code': 'PCN', 'name': 'Pitcairn'},
      {'code': 'PRI', 'name': 'Puerto Rico'},
      {'code': 'PSE', 'name': 'Palestine, State of'},
      {'code': 'PRT', 'name': 'Portugal'},
      {'code': 'PLW', 'name': 'Palau'},
      {'code': 'PRY', 'name': 'Paraguay'},
      {'code': 'QAT', 'name': 'Qatar'},
      {'code': 'REU', 'name': 'Réunion'},
      {'code': 'ROU', 'name': 'Romania'},
      {'code': 'SRB', 'name': 'Serbia'},
      {'code': 'RUS', 'name': 'Russian Federation'},
      {'code': 'RWA', 'name': 'Rwanda'},
      {'code': 'SAU', 'name': 'Saudi Arabia'},
      {'code': 'SLB', 'name': 'Solomon Islands'},
      {'code': 'SYC', 'name': 'Seychelles'},
      {'code': 'SDN', 'name': 'Sudan'},
      {'code': 'SWE', 'name': 'Sweden'},
      {'code': 'SGP', 'name': 'Singapore'},
      {'code': 'SHN', 'name': 'Saint Helena, Ascension and Tristan da Cunha'},
      {'code': 'SVN', 'name': 'Slovenia'},
      {'code': 'SJM', 'name': 'Svalbard and Jan Mayen'},
      {'code': 'SVK', 'name': 'Slovakia'},
      {'code': 'SLE', 'name': 'Sierra Leone'},
      {'code': 'SMR', 'name': 'San Marino'},
      {'code': 'SEN', 'name': 'Senegal'},
      {'code': 'SOM', 'name': 'Somalia'},
      {'code': 'SUR', 'name': 'Suriname'},
      {'code': 'SSD', 'name': 'South Sudan'},
      {'code': 'STP', 'name': 'Sao Tome and Principe'},
      {'code': 'SLV', 'name': 'El Salvador'},
      {'code': 'SXM', 'name': 'Sint Maarten (Dutch part)'},
      {'code': 'SYR', 'name': 'Syrian Arab Republic'},
      {'code': 'SWZ', 'name': 'Swaziland'},
      {'code': 'TCA', 'name': 'Turks and Caicos Islands'},
      {'code': 'TCD', 'name': 'Chad'},
      {'code': 'ATF', 'name': 'French Southern Territories'},
      {'code': 'TGO', 'name': 'Togo'},
      {'code': 'THA', 'name': 'Thailand'},
      {'code': 'TJK', 'name': 'Tajikistan'},
      {'code': 'TKL', 'name': 'Tokelau'},
      {'code': 'TLS', 'name': 'Timor-Leste'},
      {'code': 'TKM', 'name': 'Turkmenistan'},
      {'code': 'TUN', 'name': 'Tunisia'},
      {'code': 'TON', 'name': 'Tonga'},
      {'code': 'TUR', 'name': 'Turkey'},
      {'code': 'TTO', 'name': 'Trinidad and Tobago'},
      {'code': 'TUV', 'name': 'Tuvalu'},
      {'code': 'TWN', 'name': 'Taiwan, Province of China'},
      {'code': 'TZA', 'name': 'Tanzania, United Republic of'},
      {'code': 'UKR', 'name': 'Ukraine'},
      {'code': 'UGA', 'name': 'Uganda'},
      {'code': 'UMI', 'name': 'United States Minor Outlying Islands'},
      {'code': 'USA', 'name': 'United States of America'},
      {'code': 'URY', 'name': 'Uruguay'},
      {'code': 'UZB', 'name': 'Uzbekistan'},
      {'code': 'VAT', 'name': 'Holy See'},
      {'code': 'VCT', 'name': 'Saint Vincent and the Grenadines'},
      {'code': 'VEN', 'name': 'Venezuela, Bolivarian Republic of'},
      {'code': 'VGB', 'name': 'Virgin Islands, British'},
      {'code': 'VIR', 'name': 'Virgin Islands, U.S.'},
      {'code': 'VNM', 'name': 'Viet Nam'},
      {'code': 'VUT', 'name': 'Vanuatu'},
      {'code': 'WLF', 'name': 'Wallis and Futuna'},
      {'code': 'WSM', 'name': 'Samoa'},
      {'code': 'YEM', 'name': 'Yemen'},
      {'code': 'MYT', 'name': 'Mayotte'},
      {'code': 'ZAF', 'name': 'South Africa'},
      {'code': 'ZMB', 'name': 'Zambia'},
      {'code': 'ZWE', 'name': 'Zimbabwe'}
    ],
    'fr': [
      {'code': 'AND', 'name': 'Andorre'},
      {'code': 'ARE', 'name': 'Émirats arabes unis'},
      {'code': 'AFG', 'name': 'Afghanistan'},
      {'code': 'ATG', 'name': 'Antigua-et-Barbuda'},
      {'code': 'AIA', 'name': 'Anguilla'},
      {'code': 'ALB', 'name': 'Albanie'},
      {'code': 'ARM', 'name': 'Arménie'},
      {'code': 'AGO', 'name': 'Angola'},
      {'code': 'ATA', 'name': 'Antarctique'},
      {'code': 'ARG', 'name': 'Argentine'},
      {'code': 'ASM', 'name': 'Samoa américaines'},
      {'code': 'AUT', 'name': 'Autriche'},
      {'code': 'AUS', 'name': 'Australie'},
      {'code': 'ABW', 'name': 'Aruba'},
      {'code': 'ALA', 'name': 'Îles Åland'},
      {'code': 'AZE', 'name': 'Azerbaïdjan'},
      {'code': 'BIH', 'name': 'Bosnie-Herzégovine'},
      {'code': 'BRB', 'name': 'Barbade'},
      {'code': 'BGD', 'name': 'Bangladesh'},
      {'code': 'BEL', 'name': 'Belgique'},
      {'code': 'BFA', 'name': 'Burkina Faso'},
      {'code': 'BGR', 'name': 'Bulgarie'},
      {'code': 'BHR', 'name': 'Bahreïn'},
      {'code': 'BDI', 'name': 'Burundi'},
      {'code': 'BEN', 'name': 'Bénin'},
      {'code': 'BLM', 'name': 'Saint-Barthélemy'},
      {'code': 'BMU', 'name': 'Bermudes'},
      {'code': 'BRN', 'name': 'Brunei'},
      {'code': 'BOL', 'name': 'Bolivie'},
      {'code': 'BES', 'name': 'Pays-Bas caribéens'},
      {'code': 'BRA', 'name': 'Brésil'},
      {'code': 'BHS', 'name': 'Bahamas'},
      {'code': 'BTN', 'name': 'Bhoutan'},
      {'code': 'BVT', 'name': 'Île Bouvet'},
      {'code': 'BWA', 'name': 'Botswana'},
      {'code': 'BLR', 'name': 'Biélorussie'},
      {'code': 'BLZ', 'name': 'Belize'},
      {'code': 'CAN', 'name': 'Canada'},
      {'code': 'CCK', 'name': 'Îles Cocos'},
      {'code': 'COD', 'name': 'République démocratique du Congo'},
      {'code': 'CAF', 'name': 'République centrafricaine'},
      {'code': 'COG', 'name': 'République du Congo'},
      {'code': 'CHE', 'name': 'Suisse'},
      {'code': 'CIV', 'name': 'Côte d\'Ivoire'},
      {'code': 'COK', 'name': 'Îles Cook'},
      {'code': 'CHL', 'name': 'Chili'},
      {'code': 'CMR', 'name': 'Cameroun'},
      {'code': 'CHN', 'name': 'Chine'},
      {'code': 'COL', 'name': 'Colombie'},
      {'code': 'CRI', 'name': 'Costa Rica'},
      {'code': 'CUB', 'name': 'Cuba'},
      {'code': 'CPV', 'name': 'Cap-Vert'},
      {'code': 'CUW', 'name': 'Curaçao'},
      {'code': 'CXR', 'name': 'Île Christmas'},
      {'code': 'CYP', 'name': 'Chypre (pays)'},
      {'code': 'CZE', 'name': 'République tchèque'},
      {'code': 'DEU', 'name': 'Allemagne'},
      {'code': 'DJI', 'name': 'Djibouti'},
      {'code': 'DNK', 'name': 'Danemark'},
      {'code': 'DMA', 'name': 'Dominique'},
      {'code': 'DOM', 'name': 'République dominicaine'},
      {'code': 'DZA', 'name': 'Algérie'},
      {'code': 'ECU', 'name': 'Équateur (pays)'},
      {'code': 'EST', 'name': 'Estonie'},
      {'code': 'EGY', 'name': 'Égypte'},
      {'code': 'ESH', 'name': 'République arabe sahraouie démocratique'},
      {'code': 'ERI', 'name': 'Érythrée'},
      {'code': 'ESP', 'name': 'Espagne'},
      {'code': 'ETH', 'name': 'Éthiopie'},
      {'code': 'FIN', 'name': 'Finlande'},
      {'code': 'FJI', 'name': 'Fidji'},
      {'code': 'FLK', 'name': 'Malouines'},
      {'code': 'FSM', 'name': 'Micronésie (pays)'},
      {'code': 'FRO', 'name': 'Îles Féroé'},
      {'code': 'FRA', 'name': 'France'},
      {'code': 'GAB', 'name': 'Gabon'},
      {'code': 'GBR', 'name': 'Royaume-Uni'},
      {'code': 'GRD', 'name': 'Grenade (pays)'},
      {'code': 'GEO', 'name': 'Géorgie (pays)'},
      {'code': 'GUF', 'name': 'Guyane'},
      {'code': 'GGY', 'name': 'Guernesey'},
      {'code': 'GHA', 'name': 'Ghana'},
      {'code': 'GIB', 'name': 'Gibraltar'},
      {'code': 'GRL', 'name': 'Groenland'},
      {'code': 'GMB', 'name': 'Gambie'},
      {'code': 'GIN', 'name': 'Guinée'},
      {'code': 'GLP', 'name': 'Guadeloupe'},
      {'code': 'GNQ', 'name': 'Guinée équatoriale'},
      {'code': 'GRC', 'name': 'Grèce'},
      {'code': 'SGS', 'name': 'Géorgie du Sud-et-les Îles Sandwich du Sud'},
      {'code': 'GTM', 'name': 'Guatemala'},
      {'code': 'GUM', 'name': 'Guam'},
      {'code': 'GNB', 'name': 'Guinée-Bissau'},
      {'code': 'GUY', 'name': 'Guyana'},
      {'code': 'HKG', 'name': 'Hong Kong'},
      {'code': 'HMD', 'name': 'Îles Heard-et-MacDonald'},
      {'code': 'HND', 'name': 'Honduras'},
      {'code': 'HRV', 'name': 'Croatie'},
      {'code': 'HTI', 'name': 'Haïti'},
      {'code': 'HUN', 'name': 'Hongrie'},
      {'code': 'IDN', 'name': 'Indonésie'},
      {'code': 'IRL', 'name': 'Irlande (pays)'},
      {'code': 'ISR', 'name': 'Israël'},
      {'code': 'IMN', 'name': 'Île de Man'},
      {'code': 'IND', 'name': 'Inde'},
      {'code': 'IOT', 'name': 'Territoire britannique de l\'océan Indien'},
      {'code': 'IRQ', 'name': 'Irak'},
      {'code': 'IRN', 'name': 'Iran'},
      {'code': 'ISL', 'name': 'Islande'},
      {'code': 'ITA', 'name': 'Italie'},
      {'code': 'JEY', 'name': 'Jersey'},
      {'code': 'JAM', 'name': 'Jamaïque'},
      {'code': 'JOR', 'name': 'Jordanie'},
      {'code': 'JPN', 'name': 'Japon'},
      {'code': 'KEN', 'name': 'Kenya'},
      {'code': 'KGZ', 'name': 'Kirghizistan'},
      {'code': 'KHM', 'name': 'Cambodge'},
      {'code': 'KIR', 'name': 'Kiribati'},
      {'code': 'COM', 'name': 'Comores (pays)'},
      {'code': 'KNA', 'name': 'Saint-Christophe-et-Niévès'},
      {'code': 'PRK', 'name': 'Corée du Nord'},
      {'code': 'KOR', 'name': 'Corée du Sud'},
      {'code': 'KWT', 'name': 'Koweït'},
      {'code': 'CYM', 'name': 'Îles Caïmans'},
      {'code': 'KAZ', 'name': 'Kazakhstan'},
      {'code': 'LAO', 'name': 'Laos'},
      {'code': 'LBN', 'name': 'Liban'},
      {'code': 'LCA', 'name': 'Sainte-Lucie'},
      {'code': 'LIE', 'name': 'Liechtenstein'},
      {'code': 'LKA', 'name': 'Sri Lanka'},
      {'code': 'LBR', 'name': 'Liberia'},
      {'code': 'LSO', 'name': 'Lesotho'},
      {'code': 'LTU', 'name': 'Lituanie'},
      {'code': 'LUX', 'name': 'Luxembourg (pays)'},
      {'code': 'LVA', 'name': 'Lettonie'},
      {'code': 'LBY', 'name': 'Libye'},
      {'code': 'MAR', 'name': 'Maroc'},
      {'code': 'MCO', 'name': 'Monaco'},
      {'code': 'MDA', 'name': 'Moldavie'},
      {'code': 'MNE', 'name': 'Monténégro'},
      {'code': 'MAF', 'name': 'Saint-Martin'},
      {'code': 'MDG', 'name': 'Madagascar'},
      {'code': 'MHL', 'name': 'Îles Marshall (pays)'},
      {'code': 'MKD', 'name': 'Macédoine (pays)'},
      {'code': 'MLI', 'name': 'Mali'},
      {'code': 'MMR', 'name': 'Birmanie'},
      {'code': 'MNG', 'name': 'Mongolie'},
      {'code': 'MAC', 'name': 'Macao'},
      {'code': 'MNP', 'name': 'Îles Mariannes du Nord'},
      {'code': 'MTQ', 'name': 'Martinique'},
      {'code': 'MRT', 'name': 'Mauritanie'},
      {'code': 'MSR', 'name': 'Montserrat'},
      {'code': 'MLT', 'name': 'Malte'},
      {'code': 'MUS', 'name': 'Maurice (pays)'},
      {'code': 'MDV', 'name': 'Maldives'},
      {'code': 'MWI', 'name': 'Malawi'},
      {'code': 'MEX', 'name': 'Mexique'},
      {'code': 'MYS', 'name': 'Malaisie'},
      {'code': 'MOZ', 'name': 'Mozambique'},
      {'code': 'NAM', 'name': 'Namibie'},
      {'code': 'NCL', 'name': 'Nouvelle-Calédonie'},
      {'code': 'NER', 'name': 'Niger'},
      {'code': 'NFK', 'name': 'Île Norfolk'},
      {'code': 'NGA', 'name': 'Nigeria'},
      {'code': 'NIC', 'name': 'Nicaragua'},
      {'code': 'NLD', 'name': 'Pays-Bas'},
      {'code': 'NOR', 'name': 'Norvège'},
      {'code': 'NPL', 'name': 'Népal'},
      {'code': 'NRU', 'name': 'Nauru'},
      {'code': 'NIU', 'name': 'Niue'},
      {'code': 'NZL', 'name': 'Nouvelle-Zélande'},
      {'code': 'OMN', 'name': 'Oman'},
      {'code': 'PAN', 'name': 'Panama'},
      {'code': 'PER', 'name': 'Pérou'},
      {'code': 'PYF', 'name': 'Polynésie française'},
      {'code': 'PNG', 'name': 'Papouasie-Nouvelle-Guinée'},
      {'code': 'PHL', 'name': 'Philippines'},
      {'code': 'PAK', 'name': 'Pakistan'},
      {'code': 'POL', 'name': 'Pologne'},
      {'code': 'SPM', 'name': 'Saint-Pierre-et-Miquelon'},
      {'code': 'PCN', 'name': 'Îles Pitcairn'},
      {'code': 'PRI', 'name': 'Porto Rico'},
      {'code': 'PSE', 'name': 'Palestine'},
      {'code': 'PRT', 'name': 'Portugal'},
      {'code': 'PLW', 'name': 'Palaos'},
      {'code': 'PRY', 'name': 'Paraguay'},
      {'code': 'QAT', 'name': 'Qatar'},
      {'code': 'REU', 'name': 'La Réunion'},
      {'code': 'ROU', 'name': 'Roumanie'},
      {'code': 'SRB', 'name': 'Serbie'},
      {'code': 'RUS', 'name': 'Russie'},
      {'code': 'RWA', 'name': 'Rwanda'},
      {'code': 'SAU', 'name': 'Arabie saoudite'},
      {'code': 'SLB', 'name': 'Salomon'},
      {'code': 'SYC', 'name': 'Seychelles'},
      {'code': 'SDN', 'name': 'Soudan'},
      {'code': 'SWE', 'name': 'Suède'},
      {'code': 'SGP', 'name': 'Singapour'},
      {'code': 'SHN', 'name': 'Sainte-Hélène, Ascension et Tristan da Cunha'},
      {'code': 'SVN', 'name': 'Slovénie'},
      {'code': 'SJM', 'name': 'Svalbard et ile Jan Mayen'},
      {'code': 'SVK', 'name': 'Slovaquie'},
      {'code': 'SLE', 'name': 'Sierra Leone'},
      {'code': 'SMR', 'name': 'Saint-Marin'},
      {'code': 'SEN', 'name': 'Sénégal'},
      {'code': 'SOM', 'name': 'Somalie'},
      {'code': 'SUR', 'name': 'Suriname'},
      {'code': 'SSD', 'name': 'Soudan du Sud'},
      {'code': 'STP', 'name': 'Sao Tomé-et-Principe'},
      {'code': 'SLV', 'name': 'Salvador'},
      {'code': 'SXM', 'name': 'Sint Maarten'},
      {'code': 'SYR', 'name': 'Syrie'},
      {'code': 'SWZ', 'name': 'Swaziland'},
      {'code': 'TCA', 'name': 'Îles Turques-et-Caïques'},
      {'code': 'TCD', 'name': 'Tchad'},
      {'code': 'ATF', 'name': 'Terres australes et antarctiques françaises'},
      {'code': 'TGO', 'name': 'Togo'},
      {'code': 'THA', 'name': 'Thaïlande'},
      {'code': 'TJK', 'name': 'Tadjikistan'},
      {'code': 'TKL', 'name': 'Tokelau'},
      {'code': 'TLS', 'name': 'Timor oriental'},
      {'code': 'TKM', 'name': 'Turkménistan'},
      {'code': 'TUN', 'name': 'Tunisie'},
      {'code': 'TON', 'name': 'Tonga'},
      {'code': 'TUR', 'name': 'Turquie'},
      {'code': 'TTO', 'name': 'Trinité-et-Tobago'},
      {'code': 'TUV', 'name': 'Tuvalu'},
      {'code': 'TWN', 'name': 'Taïwan / (République de Chine (Taïwan))'},
      {'code': 'TZA', 'name': 'Tanzanie'},
      {'code': 'UKR', 'name': 'Ukraine'},
      {'code': 'UGA', 'name': 'Ouganda'},
      {'code': 'UMI', 'name': 'Îles mineures éloignées des États-Unis'},
      {'code': 'USA', 'name': 'États-Unis'},
      {'code': 'URY', 'name': 'Uruguay'},
      {'code': 'UZB', 'name': 'Ouzbékistan'},
      {'code': 'VAT', 'name': 'Saint-Siège (État de la Cité du Vatican)'},
      {'code': 'VCT', 'name': 'Saint-Vincent-et-les Grenadines'},
      {'code': 'VEN', 'name': 'Venezuela'},
      {'code': 'VGB', 'name': 'Îles Vierges britanniques'},
      {'code': 'VIR', 'name': 'Îles Vierges des États-Unis'},
      {'code': 'VNM', 'name': 'Viêt Nam'},
      {'code': 'VUT', 'name': 'Vanuatu'},
      {'code': 'WLF', 'name': 'Wallis-et-Futuna'},
      {'code': 'WSM', 'name': 'Samoa'},
      {'code': 'YEM', 'name': 'Yémen'},
      {'code': 'MYT', 'name': 'Mayotte'},
      {'code': 'ZAF', 'name': 'Afrique du Sud'},
      {'code': 'ZMB', 'name': 'Zambie'},
      {'code': 'ZWE', 'name': 'Zimbabwe'}
    ]
  })

  .service('CountriesIsoUtils', ['$log','ObibaCountriesIsoCodes',
    function($log, ObibaCountriesIsoCodes) {
      this.findByCode = function(code, locale) {
        var theLocale = locale || 'en';

        if (!(theLocale in ObibaCountriesIsoCodes)) {
          $log.error('ng-obiba: Invalid locale ', locale);
          return code;
        }

        var filtered = ObibaCountriesIsoCodes[theLocale].filter(function (country) {
          return country.code === code;
        });

        if (filtered && filtered.length > 0) {
          return filtered[0].name;
        }

        $log.error('ng-obiba: Invalid name ', code);
        return code;
      };

      this.findByName = function(name, locale) {
        var theLocale = locale || 'en';
        if (!(theLocale in ObibaCountriesIsoCodes)) {
          $log.error('ng-obiba: Invalid locale ', locale);
          return name;
        }

        var filtered = ObibaCountriesIsoCodes[theLocale].filter(function (country) {
          return country.name.toLowerCase() === name.toLowerCase();
        });

        if (filtered && filtered.length > 0) {
          return filtered[0].code;
        }
        
        $log.error('ng-obiba: Invalid name ', name);
        return name;
      };
    }])

  .service('StringUtils', function () {
    this.capitaliseFirstLetter = function (string) {
      return string ? string.charAt(0).toUpperCase() + string.slice(1) : null;
    };

    this.replaceAll = function(str, mapObj) {
      var re = new RegExp(Object.keys(mapObj).join('|'),'gi');

      return str.replace(re, function(matched){
        return mapObj[matched.toLowerCase()];
      });
    };

    this.truncate = function (text, size) {
      var max = size || 30;
      return text.length > max ? text.substring(0, max) + '...' : text;
    };
  })

  .service('LocaleStringUtils', ['$filter', function ($filter) {
    this.translate = function (key, args) {

      function buildMessageArguments(args) {
        if (args && args instanceof Array) {
          var messageArgs = {};
          args.forEach(function (arg, index) {
            messageArgs['arg' + index] = arg;
          });

          return messageArgs;
        }

        return {};
      }

      return $filter('translate')(key, buildMessageArguments(args));
    };
  }])

  .service('ServerErrorUtils', ['LocaleStringUtils', function (LocaleStringUtils) {
    this.buildMessage = function (response) {
      var message = null;
      var data = response.data ? response.data : response;

      if (data) {
        if (data.messageTemplate) {
          message = LocaleStringUtils.translate(data.messageTemplate, data.arguments);
          if (message === data.messageTemplate) {
            message = null;
          }
        }

        if (!message && data.code && data.message) {
          message = data.message + ' (' + response.status + ')';
        }
      }

      return message ? message : response.statusText + ' (' + response.status + ')';
    };
  }])

  .service('JsonUtils',
    function () {
      return {
        parseJsonSafely: function (json, defaultValue) {
          try {
            return JSON.parse(json);
          } catch (e) {
            return defaultValue;
          }
        },

        prettifyJson: function (jsonData) {
          var str = typeof jsonData === 'string' ? jsonData : JSON.stringify(jsonData, undefined, 2);
          return str;
        },

        isJsonValid: function (json) {
          try {
            JSON.parse(json);
          } catch (e) {
            return false;
          }
          return true;
        }
      };
  });
;'use strict';

angular.module('obiba.notification', [
  'templates-main',
  'pascalprecht.translate',
  'ui.bootstrap'
]);
;'use strict';

angular.module('obiba.notification')

  .constant('NOTIFICATION_EVENTS', {
    showNotificationDialog: 'event:show-notification-dialog',
    showConfirmDialog: 'event:show-confirmation-dialog',
    confirmDialogAccepted: 'event:confirmation-accepted',
    confirmDialogRejected: 'event:confirmation-rejected'
  })

  .controller('NotificationController', ['$rootScope', '$scope', '$uibModal', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, $uibModal, NOTIFICATION_EVENTS) {

      $scope.$on(NOTIFICATION_EVENTS.showNotificationDialog, function (event, notification) {
        $uibModal.open({
          templateUrl: 'notification/notification-modal.tpl.html',
          controller: 'NotificationModalController',
          resolve: {
            notification: function () {
              return notification;
            }
          }
        });
      });

      $scope.$on(NOTIFICATION_EVENTS.showConfirmDialog, function (event, confirm, args) {
        $uibModal.open({
          templateUrl: 'notification/notification-confirm-modal.tpl.html',
          controller: 'NotificationConfirmationController',
          resolve: {
            confirm: function () {
              return confirm;
            }
          }
        }).result.then(function () {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.confirmDialogAccepted, args);
          }, function () {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.confirmDialogRejected, args);
          });
      });

    }])

  .controller('NotificationModalController', ['$scope', '$uibModalInstance', 'notification',
    function ($scope, $uibModalInstance, notification) {

      $scope.notification = notification;
      if (!$scope.notification.iconClass) {
        $scope.notification.iconClass = 'fa-exclamation-triangle';
      }
      if (!$scope.notification.title && !$scope.notification.titleKey) {
        $scope.notification.titleKey = 'error';
      }

      $scope.close = function () {
        $uibModalInstance.dismiss('close');
      };

    }])

  .controller('NotificationConfirmationController', ['$scope', '$uibModalInstance', 'confirm', 'LocaleStringUtils',
    function ($scope, $uibModalInstance, confirm, LocaleStringUtils) {

      function getMessage() {
        return {
          title: confirm.titleKey ? LocaleStringUtils.translate(confirm.titleKey) : confirm.title,
          message: confirm.messageKey ? LocaleStringUtils.translate(confirm.messageKey, confirm.messageArgs) : confirm.message
        };
      }

      $scope.confirm = getMessage();

      $scope.ok = function () {
        $uibModalInstance.close();
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }]);

;'use strict';

angular.module('obiba.rest', ['obiba.notification'])

  .config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('httpErrorsInterceptor');
  }])

  .factory('httpErrorsInterceptor', ['$q', '$rootScope', 'NOTIFICATION_EVENTS', 'ServerErrorUtils',
    function ($q, $rootScope, NOTIFICATION_EVENTS, ServerErrorUtils) {
      return function (promise) {
        return promise.then(
          function (response) {
            // $log.debug('httpErrorsInterceptor success', response);
            return response;
          },
          function (response) {
            // $log.debug('httpErrorsInterceptor error', response);
            var config = response.config;
            if (config.errorHandler) {
              return $q.reject(response);
            }
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
              message: ServerErrorUtils.buildMessage(response)
            });
            return $q.reject(response);
          });
      };

    }]);
;'use strict';

angular.module('obiba.form', [
  'obiba.utils',
  'obiba.notification',
  'templates-main'
]);
;'use strict';

angular.module('obiba.form')

  .service('FormServerValidation', ['$rootScope', '$log', 'StringUtils', 'ServerErrorUtils', 'NOTIFICATION_EVENTS',
    function ($rootScope, $log, StringUtils, ServerErrorUtils, NOTIFICATION_EVENTS) {
      this.error = function (response, form, languages) {

        if (response.data instanceof Array) {

          var setFieldError = function (field, error) {
            form[field].$dirty = true;
            form[field].$setValidity('server', false);
            if (!form[field].errors) {
              form[field].errors = [];
            }
            form[field].errors.push(StringUtils.capitaliseFirstLetter(error.message));
          };

          response.data.forEach(function (error) {
            var fieldPrefix = error.path.split('.').slice(-2).join('.');
            if (languages && languages.length) {
              languages.forEach(function (lang) {
                setFieldError(fieldPrefix + '-' + lang, error);
              });
            } else {
              setFieldError(fieldPrefix, error);
            }
          });
          $log.debug(form);
        } else {
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            titleKey: 'form-server-error',
            message: ServerErrorUtils.buildMessage(response)
          });
        }

      };
    }])

  .service('RadioGroupOptionBuilder', function() {
    this.build = function(prefix, items) {
      return items.map(function(item) {
        return {
          name: prefix,
          label: item.label || item,
          value: item.name
        };
      });
    };

    return this;
  });;'use strict';

angular.module('obiba.form')

  // http://codetunes.com/2013/server-form-validation-with-angular
  .directive('formServerError', [function () {
    return {
      restrict: 'A',
      require: '?ngModel',
      link: function (scope, element, attrs, ctrl) {
        return element.on('change', function () {
          return scope.$apply(function () {
            return ctrl.$setValidity('server', true);
          });
        });
      }
    };
  }])

  .directive('formInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        disabled: '=',
        type: '@',
        pattern: '=',
        label: '@',
        required: '=',
        min: '@',
        max: '@',
        step: '@',
        help: '@',
        placeholder: '@',
        readonly: '@'
      },
      templateUrl: 'form/form-input-template.tpl.html',
      compile: function(elem, attrs) {
        if (!attrs.type) { attrs.type = 'text'; }
        return {
          post: function (scope, elem, attr, ctrl) {
            scope.form = ctrl;
          }
        };
      }
    };
  }])

  .directive('formTextarea', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        disabled: '=',
        label: '@',
        required: '=',
        help: '@'
      },
      templateUrl: 'form/form-textarea-template.tpl.html',
      compile: function(elem, attrs) {
        if (!attrs.type) { attrs.type = 'text'; }
        return {
          post: function ($scope, elem, attr, ctrl) {
            $scope.form = ctrl;
          }
        };
      }
    };
  }])

  .directive('formLocalizedInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        locales: '=',
        name: '@',
        model: '=',
        label: '@',
        required: '=',
        help: '@'
      },
      templateUrl: 'form/form-localized-input-template.tpl.html',
      link: function ($scope, elem, attr, ctrl) {
        $scope.form = ctrl;
      }
    };
  }])

  .directive('formRadio', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        gid: '@',
        model: '=',
        value: '=',
        label: '@',
        help: '@'
      },
      templateUrl: 'form/form-radio-template.tpl.html',
      link: function ($scope, elem, attr, ctrl) {
        $scope.form = ctrl;
      }
    };
  }])

  .directive('formRadioGroup', [function() {
    return {
      restrict: 'AE',
      scope: {
        options: '=',
        model: '='
      },
      templateUrl: 'form/form-radio-group-template.tpl.html',
      link: function ($scope) {
        $scope.gid = $scope.$id;
      }
    };
  }])

  .directive('formCheckbox', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        gid: '@',
        model: '=',
        required: '=',
        disabled: '=',
        label: '@',
        help: '@'
      },
      templateUrl: 'form/form-checkbox-template.tpl.html',
      link: function ($scope, elem, attr, ctrl) {
        $scope.form = ctrl;
      }
    };
  }])

  .directive('formCheckboxGroup', [function() {
    return {
      restrict: 'A',
      scope: {
        options: '=',
        model: '='
      },
      template: '<div form-checkbox ng-repeat="item in items" name="{{item.name}}" model="item.value" gid="${{gid}}" label="{{item.label}}">',
      link: function ($scope, elem, attrs) {
        $scope.gid = $scope.$id;
        $scope.$watch('model', function(selected) {
          if (!selected || !$scope.options) {
            return;
          }

          $scope.items = $scope.options.map(function(n) {
            var value = angular.isArray(selected) && (selected.indexOf(n) > -1 ||
              selected.indexOf(n.name) > -1);
            return {
              name: attrs.model + '.' + (n.name || n),
              label: n.label || n,
              value: value
            };
          });
        }, true);

        $scope.$watch('items', function(items) {
          if (items && angular.isArray(items)) {
            $scope.model = items.filter(function(e) { return e.value; })
              .map(function(e) { return e.name.replace(attrs.model + '.', ''); });
          }
        }, true);

        $scope.$watch('options', function(opts) {
          if (!opts) {
            return;
          }

          $scope.items = opts.map(function(n) {
            var value = angular.isArray($scope.model) && ($scope.model.indexOf(n) > -1 ||
              $scope.model.indexOf(n.name) > -1);
            return {
              name: attrs.model + '.' + (n.name || n),
              label: n.label || n,
              value: value
            };
          });
        }, true);
      }
    };
  }]);
;'use strict';

angular.module('obiba.alert', [
  'templates-main',
  'pascalprecht.translate',
  'ui.bootstrap',
  'ngSanitize'
]);
;'use strict';

angular.module('obiba.alert')

  .constant('ALERT_EVENTS', {
    showAlert: 'event:show-alert'
  })

  .service('AlertService', ['$rootScope', '$log', 'LocaleStringUtils', 'ALERT_EVENTS',
    function ($rootScope, $log, LocaleStringUtils, ALERT_EVENTS) {

      function getValidMessage(options) {
        var value = LocaleStringUtils.translate(options.msgKey, options.msgArgs);
        if (value === options.msgKey) {
          if (options.msg) {
            return options.msg;
          }

          $log.error('No message was provided for the alert!');
          return '';
        }

        return value;
      }

      function broadcast(options, growl) {
        $rootScope.$broadcast(ALERT_EVENTS.showAlert, {
          uid: new Date().getTime(), // useful for delay closing and cleanup
          message: getValidMessage(options),
          type: options.type ? options.type : 'info',
          growl: growl,
          timeoutDelay: options.delay ? Math.max(0, options.delay) : 0
        }, options.id);
      }

      this.alert = function (options) {
        broadcast(options);
      };

      this.growl = function(options) {
        broadcast(options, true);
      };
    }]);
;'use strict';

angular.module('obiba.alert')

  .directive('obibaAlert', ['$rootScope', '$timeout', '$log', 'ALERT_EVENTS',
    function ($rootScope, $timeout, $log, ALERT_EVENTS) {

      return {
        restrict: 'AE',
        scope: {
          id: '@'
        },
        templateUrl: 'alert/alert-template.tpl.html',
        link: function(scope) {
          scope.alerts = [];
          if (!scope.id) {
            throw new Error('ObibaAlert directive must have a DOM id attribute.');
          }

          scope.close = function(index) {
            scope.alerts.splice(index, 1);
          };

          /**
           * Called when timeout has expired
           * @param uid
           */
          scope.closeByUid = function(uid) {
            var index = scope.alerts.map(function(alert) {
              return alert.uid === uid;
            }).indexOf(true);

            if (index !== -1) {
              scope.close(index);
            }
          };

          scope.$on(ALERT_EVENTS.showAlert, function (event, alert, id) {
            if (scope.id === id) {
              scope.alerts.push(alert);
              if (alert.timeoutDelay > 0) {
                $timeout(scope.closeByUid.bind(null, alert.uid), alert.timeoutDelay);
              }
            }
          });
        }
    };
  }]);
;'use strict';

angular.module('obiba.comments', [
  'obiba.utils',
  'obiba.notification',
  'obiba.form',
  'templates-main',
  'hc.marked',
  'pascalprecht.translate',
  'angularMoment'
]);
;'use strict';

angular.module('obiba.comments')

  .config(['markedProvider', function(markedProvider) {
    markedProvider.setOptions({
      gfm: true,
      tables: true,
      sanitize: true
    });
  }])

  .filter('fromNow', ['moment', function(moment) {
    return function(dateString) {
      return moment(dateString).fromNow();
    };
  }])

  .directive('obibaCommentEditor', [function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        onSubmit: '&',
        onCancel: '&',
        comment: '=?'
      },
      templateUrl: 'comments/comment-editor-template.tpl.html',
      controller: 'ObibaCommentEditorController',
      link: function(scope, elem, attrs) {
        scope.isCancellable = angular.isDefined(attrs.onCancel);
      }
    };
  }])

  .controller('ObibaCommentEditorController', ['$scope',
    function ($scope) {
      var reset = function() {
        $scope.comment = {message: null};
      };

      if (!$scope.comment) {
        reset();
      }

      $scope.cancel = function() {
        $scope.onCancel()();
      };
      $scope.send = function() {
        $scope.onSubmit()($scope.comment);
        reset();
      };
    }])


  .directive('obibaComments', [function () {
    return {
      restrict: 'E',
      scope: {
        comments: '=',
        onDelete: '&',
        onUpdate: '&',
        nameResolver: '&',
        editAction: '@',
        deleteAction: '@'
      },
      templateUrl: 'comments/comments-template.tpl.html',
      controller: 'ObibaCommentsController'
    };
  }])

  .controller('ObibaCommentsController', ['$scope',
    function ($scope) {

      var clearSelected = function(){
        $scope.selected = -1;
      };
      var canDoAction = function(comment, action) {
        return angular.isUndefined(action) || (!angular.isUndefined(comment.actions) && comment.actions.indexOf (action) !== -1);
      };

      $scope.canEdit = function(index) {
        return canDoAction($scope.comments[index], $scope.editAction);
      };
      $scope.canDelete = function(index) {
        return canDoAction($scope.comments[index], $scope.deleteAction);
      };
      $scope.submit = function(comment) {
        $scope.onUpdate()(comment);
        clearSelected();
      };
      $scope.edit = function(index) {
        $scope.selected = index;
      };
      $scope.cancel = function() {
        clearSelected();
      };
      $scope.remove = function(index) {
        $scope.onDelete()($scope.comments[index]);
      };
    }]);


;'use strict';

angular.module('ngObiba', [
  'obiba.form',
  'obiba.notification',
  'obiba.rest',
  'obiba.utils',
  'obiba.alert',
  'obiba.comments'
]);
;angular.module('templates-main', ['alert/alert-template.tpl.html', 'comments/comment-editor-template.tpl.html', 'comments/comments-template.tpl.html', 'form/form-checkbox-template.tpl.html', 'form/form-input-template.tpl.html', 'form/form-localized-input-template.tpl.html', 'form/form-radio-group-template.tpl.html', 'form/form-radio-template.tpl.html', 'form/form-textarea-template.tpl.html', 'notification/notification-confirm-modal.tpl.html', 'notification/notification-modal.tpl.html']);

angular.module("alert/alert-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("alert/alert-template.tpl.html",
    "<uib-alert ng-repeat=\"alert in alerts\"\n" +
    "           class=\"{{alert.growl ? 'alert-growl' : ''}}\"\n" +
    "           type=\"{{alert.type}}\"\n" +
    "           close=\"close($index)\">\n" +
    "  <span ng-bind-html=\"alert.message\"></span>\n" +
    "</uib-alert>");
}]);

angular.module("comments/comment-editor-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("comments/comment-editor-template.tpl.html",
    "<form class=\"obiba-comment-form\" name=\"form\" role=\"form\" ng-submit=\"send()\">\n" +
    "  <uib-tabset>\n" +
    "    <ul class=\"nav pull-right obiba-comment-form-marked-doc\">\n" +
    "      <li>\n" +
    "        <a class=\"obiba-comment-form-marked-doc\" href=\"//guides.github.com/features/mastering-markdown/\" target=\"_blank\">{{'comment.markdown-doc-link' | translate}}</a>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "    <uib-tab heading=\"{{'comment.write' | translate}}\">\n" +
    "      <textarea id=\"obiba-comment-form-message\" ng-model=\"comment.message\" class=\"form-control obiba-comment-form-message\" placeholder=\"{{'comment.placeholder' | translate}}\"></textarea>\n" +
    "    </uib-tab>\n" +
    "    <uib-tab heading=\"{{'comment.preview' | translate}}\">\n" +
    "      <div id=\"obiba-comment-form-marked\" class=\"obiba-comment-form-marked\" marked=\"comment.message\"></div>\n" +
    "    </uib-tab>\n" +
    "  </uib-tabset>\n" +
    "  <button ng-if=\"isCancellable\" ng-click=\"cancel\" type=\"submit\" class=\"btn btn-default obiba-comment-form-button\">\n" +
    "    <span>{{'cancel' | translate}}</span>\n" +
    "  </button>\n" +
    "\n" +
    "  <button ng-disabled=\"!comment.message\" type=\"submit\" class=\"btn btn-primary obiba-comment-form-button\">\n" +
    "    <span>{{'comment.send' | translate}}</span>\n" +
    "  </button>\n" +
    "</form>\n" +
    "");
}]);

angular.module("comments/comments-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("comments/comments-template.tpl.html",
    "<div id=\"obiba-comments\">\n" +
    "  <div class=\"obiba-comment-top-offset1 obiba-comment-bottom-offset1\" ng-repeat=\"comment in comments\">\n" +
    "    <div ng-hide=\"selected === $index\">\n" +
    "      <div class=\"panel panel-default\">\n" +
    "        <div class=\"panel-heading\">\n" +
    "          <div>\n" +
    "            <span class=\"obiba-comment-icon\"><i class=\"glyphicon glyphicon-comment\"></i></span>\n" +
    "            <span ng-if=\"!comment.modifiedBy\">{{'comment.created-by' | translate}} {{nameResolver()(comment.createdByProfile) || comment.createdBy}} {{comment.timestamps.created | fromNow }}</span>\n" +
    "            <span ng-if=\"comment.modifiedBy\"> {{'comment.modified-by' | translate}} {{nameResolver()(comment.modifiedByProfile) || comment.modifiedBy}} {{comment.timestamps.lastUpdate | fromNow }}</span>\n" +
    "            <span class=\"pull-right\">\n" +
    "              <a ng-if=\"canEdit($index)\" ng-click=\"edit($index)\"\n" +
    "                 class=\"btn btn-primary btn-xs\">\n" +
    "                <i class=\"fa fa-pencil-square-o\"></i>\n" +
    "              </a>\n" +
    "              <a ng-if=\"canDelete($index)\" ng-click=\"remove($index)\"\n" +
    "                 class=\"btn btn-danger btn-xs\">\n" +
    "                <i class=\"fa fa-trash-o\"></i>\n" +
    "              </a>\n" +
    "            </span>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "        <div class=\"panel-body\">\n" +
    "          <div marked=\"comment.message\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <obiba-comment-editor ng-show=\"selected === $index\" on-cancel=\"cancel\" on-submit=\"submit\" comment=\"comments[$index]\"></obiba-comment-editor>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("form/form-checkbox-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-checkbox-template.tpl.html",
    "<div class=\"checkbox\" ng-class=\"{'has-error': (form[fieldName].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{name}}\" class=\"control-label\">\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "    <input\n" +
    "          ng-model=\"model\"\n" +
    "          type=\"checkbox\"\n" +
    "          id=\"{{name}}\"\n" +
    "          name=\"{{name}}{{gid}}\"\n" +
    "          form-server-error\n" +
    "          ng-required=\"required\"\n" +
    "          ng-disabled=\"disabled\">\n" +
    "      {{label | translate}}\n" +
    "  </label>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "\n" +
    "");
}]);

angular.module("form/form-input-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-input-template.tpl.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[name].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{name}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "\n" +
    "  <input\n" +
    "      ng-model=\"model\"\n" +
    "      type=\"{{type}}\"\n" +
    "      class=\"form-control\"\n" +
    "      id=\"{{name}}\"\n" +
    "      name=\"{{name}}\"\n" +
    "      placeholder=\"{{placeholder | translate}}\"\n" +
    "      form-server-error\n" +
    "      ng-readonly=\"{{readonly}}\"\n" +
    "      ng-attr-min=\"{{min}}\"\n" +
    "      ng-attr-max=\"{{max}}\"\n" +
    "      ng-attr-step=\"{{step}}\"\n" +
    "      ng-pattern=\"pattern\"\n" +
    "      ng-disabled=\"disabled\"\n" +
    "      ng-required=\"required\"/>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-show=\"form[name].$error.pattern\">{{'pattern' | translate}} {{pattern}}</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("form/form-localized-input-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-localized-input-template.tpl.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[name].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "    <label for=\"{{name}}\" class=\"control-label\">\n" +
    "        {{label | translate}}\n" +
    "        <span ng-show=\"required\">*</span>\n" +
    "    </label>\n" +
    "\n" +
    "    <div class=\"input-group\" ng-repeat=\"locale in locales track by $index\">\n" +
    "        <span class=\"input-group-addon\">{{locale.lang}}</span>\n" +
    "        <input\n" +
    "                ng-model=\"locale.value\"\n" +
    "                type=\"text\"\n" +
    "                class=\"form-control\"\n" +
    "                id=\"{{name}}.{{locale.lang}}\"\n" +
    "                name=\"{{name}}.locale.lang}}\"\n" +
    "                form-server-error>\n" +
    "    </div>\n" +
    "\n" +
    "    <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "        <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "        <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "    </ul>\n" +
    "\n" +
    "    <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>");
}]);

angular.module("form/form-radio-group-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-radio-group-template.tpl.html",
    "<div form-radio ng-repeat=\"option in options\" name=\"{{option.name}}\" model=\"model.design\" value=\"option.value\"\n" +
    "     label=\"{{option.label}}\" gid=\"{{gid}}\"></div>");
}]);

angular.module("form/form-radio-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-radio-template.tpl.html",
    "<div class=\"radio\" ng-class=\"{'has-error': (form[fieldName].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label class=\"control-label\">\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "    <input ng-model=\"model\"\n" +
    "          ng-value=\"value\"\n" +
    "          type=\"radio\"\n" +
    "          id=\"{{name}}\"\n" +
    "          name=\"{{name}}{{gid}}\"\n" +
    "          form-server-error\n" +
    "          ng-required=\"required\">\n" +
    "      {{label | translate}}\n" +
    "  </label>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "\n" +
    "");
}]);

angular.module("form/form-textarea-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-textarea-template.tpl.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[name].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{name}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "\n" +
    "  <textarea\n" +
    "      ng-model=\"model\"\n" +
    "      class=\"form-control\"\n" +
    "      id=\"{{name}}\"\n" +
    "      name=\"{{name}}\"\n" +
    "      form-server-error\n" +
    "      ng-disabled=\"disabled\"\n" +
    "      ng-required=\"required\"></textarea>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("notification/notification-confirm-modal.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("notification/notification-confirm-modal.tpl.html",
    "<div class=\"modal-content\">\n" +
    "\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\" ng-click=\"cancel()\">&times;</button>\n" +
    "    <h4 class=\"modal-title\">\n" +
    "      <i class=\"fa fa-exclamation-triangle\"></i>\n" +
    "      <span ng-hide=\"confirm.title\" translate>confirmation</span>\n" +
    "      {{confirm.title}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-body\">\n" +
    "    <p>{{confirm.message}}</p>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-default\" ng-click=\"cancel()\">\n" +
    "      <span ng-hide=\"confirm.cancel\" translate>cancel</span>\n" +
    "      {{confirm.cancel}}\n" +
    "    </button>\n" +
    "    <button type=\"button\" class=\"btn btn-primary\" ng-click=\"ok()\">\n" +
    "      <span ng-hide=\"confirm.ok\" translate>ok</span>\n" +
    "      {{confirm.ok}}\n" +
    "    </button>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);

angular.module("notification/notification-modal.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("notification/notification-modal.tpl.html",
    "<div class=\"modal-content\">\n" +
    "\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\" ng-click=\"close()\">&times;</button>\n" +
    "    <h4 class=\"modal-title\">\n" +
    "      <i ng-hide=\"notification.iconClass\" class=\"fa fa-info-circle\"></i>\n" +
    "      <i ng-show=\"notification.iconClass\" class=\"fa {{notification.iconClass}}\"></i>\n" +
    "      <span ng-hide=\"notification.title\" translate>{{notification.titleKey || 'notification'}}</span>\n" +
    "      {{notification.title}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-body\">\n" +
    "    <p>{{notification.message}}</p>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-default\" ng-click=\"close()\">\n" +
    "      <span translate>close</span>\n" +
    "    </button>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);
