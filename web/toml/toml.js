import CodeFlask from 'codeflask';
import Prism from 'prismjs';

const flask = new CodeFlask('#code-editor', { language: 'toml' });

flask.addLanguage('toml', Prism.languages['toml']);