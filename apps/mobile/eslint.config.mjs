import tseslint from 'typescript-eslint';
export default tseslint.config({ ignores: ['node_modules/**', '.expo/**', 'dist/**', 'coverage/**', 'src/generated/**', 'android/**', 'ios/**'] }, ...tseslint.configs.recommended);
