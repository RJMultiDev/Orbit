// entry/src/main/ets/core/bili/BiliTypes.ets
// B 站接口 DTO（字段与 wire JSON 对齐，脱敏友好）。
// ArkTS 严格模式：所有嵌套对象都必须是有名 interface，禁止内联对象字面量类型。

export interface ApiResp {
  code: number;
  message: string;
  ttl?: number;
}

export interface NavWbiImg {
  img_url: string;
  sub_url: string;
}

export interface NavData {
  isLogin: boolean;
  mid?: number;
  uname?: string;
  face?: string;
  wbi_img?: NavWbiImg;
}

export interface NavResp extends ApiResp {
  data?: NavData;
}

export interface QrGenerateData {
  url: string;
  qrcode_key: string;
}

export interface QrGenerateResp extends ApiResp {
  data?: QrGenerateData;
}

export interface QrPollData {
  url: string;
  refresh_token: string;
  timestamp: number;
  code: number;
  message: string;
}

export interface QrPollResp extends ApiResp {
  data?: QrPollData;
}

export interface RecommendOwner {
  name: string;
  face: string;
  mid: number;
}

export interface RecommendStat {
  view: number;
  danmaku: number;
}

export interface RecommendCard {
  id: number;
  bvid: string;
  cid?: number;
  pic: string;
  title: string;
  owner?: RecommendOwner;
  stat?: RecommendStat;
  duration?: number;
}

export interface RecommendData {
  item?: RecommendCard[];
}

export interface RecommendResp extends ApiResp {
  data?: RecommendData;
}

export interface VideoStat {
  view: number;
  danmaku: number;
  like: number;
  coin: number;
  favorite: number;
  share: number;
  reply: number;
}

export interface VideoOwner {
  name: string;
  face: string;
  mid: number;
}

export interface VideoPage {
  cid: number;
  page: number;
  part: string;
  duration: number;
}

export interface VideoViewData {
  bvid: string;
  aid: number;
  cid: number;
  pic: string;
  title: string;
  desc: string;
  duration: number;
  owner?: VideoOwner;
  stat?: VideoStat;
  pages?: VideoPage[];
}

export interface VideoViewResp extends ApiResp {
  data?: VideoViewData;
}

export interface DashVideo {
  id: number;
  baseUrl: string;
  backupUrl?: string[];
  bandwidth: number;
  mimeType: string;
  codecs: string;
  width?: number;
  height?: number;
}

export interface DashAudio {
  id: number;
  baseUrl: string;
  backupUrl?: string[];
  bandwidth: number;
  mimeType: string;
  codecs: string;
}

export interface DashData {
  video?: DashVideo[];
  audio?: DashAudio[];
  duration?: number;
}

export interface DurlItem {
  url: string;
  backupUrl?: string[];
  size: number;
  length: number;
}

export interface PlayUrlData {
  quality: number;
  format: string;
  timelength: number;
  durl?: DurlItem[];
  dash?: DashData;
}

export interface PlayUrlResp extends ApiResp {
  data?: PlayUrlData;
}

export interface SearchVideoItem {
  type: string;
  bvid: string;
  title: string;
  pic?: string;
  duration?: string;
  author?: string;
  play?: number;
}

export interface SearchResultGroup {
  result_type: string;
  data?: SearchVideoItem[];
}

export interface SearchData {
  result?: SearchResultGroup[];
}

export interface SearchResp extends ApiResp {
  data?: SearchData;
}
