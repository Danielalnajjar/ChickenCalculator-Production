module.exports = {
  webpack: {
    configure: (webpackConfig) => {
      // Fix for ajv-keywords compatibility issues
      webpackConfig.module.rules.push({
        test: /\.m?js$/,
        resolve: {
          fullySpecified: false
        }
      });
      
      // Ignore source map warnings
      webpackConfig.ignoreWarnings = [
        /Failed to parse source map/,
        /these parameters are deprecated/
      ];
      
      return webpackConfig;
    }
  }
};